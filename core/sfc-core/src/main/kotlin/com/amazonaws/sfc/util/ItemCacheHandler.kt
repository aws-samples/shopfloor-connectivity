package com.amazonaws.sfc.util

import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import java.time.Instant
import java.util.function.Function


/**
 * @param T Type of cached item
 * @param I Type of optional data used for initialization of new item
 * @property supplier Function0<T?>  Function  returns a new instance for a cached item
 * @property initializer Function2<T?, I?, T?> Function that the value created by the supplier
 * and optional initialization data, returns initialized value. Default function returns input value.
 * @property isValid Function<in T?, out Boolean> Function that takes cached value and tests is it is still valid
 * default is a function that returns true
 * @property onInitializationError Function2<T?, Exception, T?> Function that gets called the initialization
 * of an item by the initializer throws an exception. The function can return a default value or (re)throw an exception.
 */
open class ItemCacheHandler<T, I>(

    // Creates new instance of cached value
    private val supplier: Function0<T?> = object : Function0<T?> {
        override fun invoke(): T? {
            return null
        }
    },

    // Called asynchronously to further initialize new cached values
    private val initializer: Function2<T?, I?, T?> = object : Function2<T?, I?, T?> {
        override fun invoke(value: T?, init: I?): T? {
            return value
        }
    },

    private val validFor: kotlin.time.Duration? = null,

    // Test if cached value is still valid
    private val isValid: Function<in T?, out Boolean> = Function {
        true
    },

    // Called when initialization of new items throw an exception
    private val onInitializationError: Function2<T?, Exception, T?> = object : Function2<T?, Exception, T?> {
        override fun invoke(value: T?, exception: Exception): T? {
            throw exception
        }
    }

) {


    private var cachedItem: Deferred<T?>? = null
    private var itemExpiresAt: Instant? = null

    // Lock that gives short  exclusive access to cached item
    private val cachedItemLock = Mutex()

    /**
     * @param init I? Optional data used to initialize new items
     * @return Deferred<T?> Deferred cached value
     */
    suspend fun getAsync(init: I? = null): Deferred<T?> = coroutineScope {

        return@coroutineScope async getAsyncScope@{

            // Short lock to get exclusive access cached entry to set the deferred result for initialization
            cachedItemLock.lock()

            try {

                // Check if initializer completed
                if (cachedItem?.isCompleted == true) {

                    val item = cachedItem?.await()
                    val itemIsNotExpired = itemExpiresAt == null || itemExpiresAt!! > systemDateTime()
                    if (itemIsNotExpired && isValid.apply(item)) {
                        return@getAsyncScope item
                    }
                    // function did compete but item was no longer valid, set to null, so it is executed again in next step
                    cachedItem = null
                }

                // Initializer not executed yet or reset due to value that was no longer valid
                if (cachedItem == null) {
                    // Execute initializer async and store deferred result so additional lookups for the
                    // same key will wait on same deferred result whilst the initialization function is executing.
                    cachedItem = async initAsyncScope@{
                        var item = supplier()
                        try {
                            item = initializer(item, init)
                            itemExpiresAt = if (validFor != null) systemDateTime().plusMillis(validFor.inWholeMilliseconds) else null
                            return@initAsyncScope item
                        } catch (ex: Exception) {
                            // something went wrong, action and value defined in onError method
                            item = onInitializationError(item, ex)
                            itemExpiresAt = if (validFor != null) systemDateTime().plusMillis(validFor.inWholeMilliseconds) else null
                            return@initAsyncScope item
                        }
                    }
                }

            } finally {
                // release exclusive access to entry
                cachedItemLock.unlock()
            }

            // await async initializer, concurrent calls will wait on same deferred result whilst
            // initialization is running
            return@getAsyncScope cachedItem?.await()
        }
    }

    suspend fun clear() {
        cachedItemLock.lock()
        cachedItem?.await()
        cachedItem = null
        cachedItemLock.unlock()
    }
}