// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function


/**
 *
 * @param K Type of key
 * @param T Type of cached values
 * @param I Type of optional data used for initialization of new items
 * @property supplier Function<K, T> Function that takes key and returns a new instance for a cached value
 * @property initializer Function3<K, T?, I?, T?> Function that takes key, the value created by the supplier
 * and optional initialization data, returns initialized value. Default function returns input value.
 * @property onInitializationError Function3<K, T?, Exception, T?> Function that gets called the initialization
 * of an item by the initializer throws an exception. The function can return a default value or (re)throw an exception.
 * @property isValid Function<in T?, out Boolean> Function that takes cached value and tests is it is still valid
 * default is a function that returns true
 */
open class LookupCacheHandler<K, T, I>(

    // Creates new instance of cached value
    private val supplier: Function<K, T>,

    // Called asynchronously to further initialize new cached values
    private val initializer: Function3<K, T?, I?, T?> = object : Function3<K, T?, I?, T?> {
        override fun invoke(key: K, value: T?, init: I?): T? {
            return value
        }
    },

    // Test if cached value is still valid
    private val isValid: Function<in T?, out Boolean> = Function { true },

    // Called when initialization of new items throw an exception
    private val onInitializationError: Function3<K, T?, Exception, T?> = object : Function3<K, T?, Exception, T?> {
        override fun invoke(key: K, value: T?, exception: Exception): T? {
            throw exception
        }
    }

) {

    // Internal concurrent hash map class  for cached value, if a value is not present an entry is created
    // holding a value that is the result of a call to the supplier function.
    private class CashedEntriesHashMap<K, T>(private val supplier: Function<in K, out T>) :
        ConcurrentHashMap<K, T>() {

        override operator fun get(key: K): T {
            return super.computeIfAbsent(key, supplier)
        }
    }

    // Internal cache entry that holds the actual cached value and a deferred result from an async call
    // to the initializer function.
    private class CachedEntry<T> {
        var value: Deferred<T?>? = null
    }


    // Cached entries
    private val cache = CashedEntriesHashMap<K, CachedEntry<T>> { CachedEntry() }

    /**
     * @param key K Key value for item
     * @param init I? Optional data used to initialize new items
     * @return Deferred<T?> Deferred cached value
     */
    suspend fun getItemAsync(key: K, init: I? = null): Deferred<T?> = coroutineScope {

        return@coroutineScope async getAsyncScope@{


            // Get an entry, if it is not in the cache one is created with the values set by the supplier
            val entry = cache[key]

            // Check if initializer completed
            if (entry.value?.isCompleted == true) {

                val value = entry.value?.await()
                if (isValid.apply(value)) {
                    return@getAsyncScope value
                }
                // function did compete but item was no longer valid, set to null, so it is executed again in next step
                entry.value = null
            }

            // Initializer not executed yet or reset due to value that was no longer valid
            if (entry.value == null) {
                // Execute initializer async and store deferred result so additional lookups for the
                // same key will wait on same deferred result whilst the initialization function is executing.
                entry.value = async initAsyncScope@{

                    var item: T? = supplier.apply(key)
                    try {
                        // set initialized value
                        item = initializer(key, item, init)
                        return@initAsyncScope item
                    } catch (ex: Exception) {
                        // something went wrong, action and value defined in onError method
                        item = onInitializationError(key, item, ex)
                        return@initAsyncScope item
                    }
                }
            }


            // await async initializer, concurrent calls will wait on same deferred result whilst
            // initialization is running
            return@getAsyncScope cache[key].value?.await()
        }
    }

    suspend fun clear() {

        cache.clear()

    }

    val keys: Set<K>
        get() = cache.keys

    val items: List<T?>
        get() = runBlocking { cache.values.map { it.value?.await() } }


}