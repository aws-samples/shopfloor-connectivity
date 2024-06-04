// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.util

import com.amazonaws.sfc.util.UrlReaderCache.CachedHttpResponse.Companion.toCachedResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration


class UrlReaderCache(private val cachePeriod: Duration, private val maxRetries: Int, private val waitBetweenReties: Duration) {

    class UrlCacheException( message : String) : Exception(message)

    internal class CachedHttpResponse(val content: String?, val status: Int, val timeStamp: Long) {

        companion object {


            @OptIn(InternalAPI::class)
            val HttpResponse.toCachedResponse: CachedHttpResponse
                get() = runBlocking {
                    CachedHttpResponse(
                        content = if (this@toCachedResponse.status == HttpStatusCode.OK)
                            this@toCachedResponse.content.toByteArray().toString(Charset.defaultCharset())
                        else
                            "",
                        status = this@toCachedResponse.status.value,
                        timeStamp = this@toCachedResponse.responseTime.timestamp
                    )
                }
        }
    }


    operator fun get(url : Url) : String?{
        return runBlocking {
            val resp = cache.getItemAsync(url).await()
            if (resp?.status != 200) {
                throw UrlCacheException("Error fetching content from ${url}, status code is ${resp?.status}")
            }
            resp.content
        }
    }

    fun remove(url : Url) {
        cache.remove(url)
    }

    private val cache = LookupCacheHandler<Url, CachedHttpResponse, Nothing>(
        supplier = { url ->

            runBlocking {
                val client = HttpClient(CIO)
                var retries = 0

                var resp = client.get(url).toCachedResponse
                while (resp.status != 200 && retries < maxRetries) {
                    resp = client.get(url).toCachedResponse
                    if (resp.status != 200) {
                        delay(waitBetweenReties)
                        retries++
                    }
                }
                resp

            }
        },
        isValid = { resp ->
            resp?.status == 200 && resp.timeStamp > Instant.now(Clock.systemUTC()).epochSecond + cachePeriod.inWholeSeconds
        }
    )

}


