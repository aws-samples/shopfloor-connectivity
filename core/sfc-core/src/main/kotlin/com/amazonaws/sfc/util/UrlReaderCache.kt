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
import java.io.File
import java.io.File.separator
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration


class UrlReaderCache(private val cachePeriod: Duration, private val maxRetries: Int, private val waitBetweenReties: Duration, val cacheDirectory: String? = null, val cacheResults: Boolean = false) {

    class UrlCacheException(message: String) : Exception(message)

    internal class CachedHttpResponse(val content: String?, val status: Int, val timeStamp: Long) {

        companion object {


            @OptIn(InternalAPI::class)
            val HttpResponse.toCachedResponse: CachedHttpResponse
                get() = runBlocking {
                    CachedHttpResponse(
                        content = if (this@toCachedResponse.status == HttpStatusCode.OK) {
                            this@toCachedResponse.content.toByteArray().toString(Charset.defaultCharset())
                        } else
                            "",
                        status = this@toCachedResponse.status.value,
                        timeStamp = this@toCachedResponse.responseTime.timestamp
                    )
                }
        }
    }

    fun generateNameFromUrl(url: String): String {

        // create md5 hsh for url
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(url.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    operator fun get(url: Url): String? {
        return runBlocking {
            val resp = try {
                cache.getItemAsync(url).await()
            } catch (e : Exception){
                null
            }

            if (resp?.status != 200) {
                if (cacheResults) {
                    val fileName = generateNameFromUrl(url.toString())
                    val cacheFile = "${File(cacheDirectory ?: currentDirectory()).path}$separator$fileName"
                    if (File(cacheFile).exists()) {
                        try {
                            val content = File(cacheFile).readText()
                            return@runBlocking content
                        } catch (e: Exception) {
                            throw UrlCacheException("Error reading from cache file $cacheFile, $e")
                        }
                    }
                }
                throw UrlCacheException("Error fetching content from ${url}, status code is ${resp?.status}")
            }
            resp.content
        }
    }


    fun remove(url: Url) {
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

        initializer = { url, resp, _ ->
            if (cacheResults && resp?.status == 200) {
                val fileName = generateNameFromUrl(url.toString())
                val cacheFile = "${File(cacheDirectory ?: currentDirectory()).path}$separator$fileName"
                try {
                    File(cacheFile).writeText(resp.content?.toByteArray()?.toString(Charset.defaultCharset()) ?: "")
                } catch (e: Exception) {
                    throw UrlCacheException("Error writing to cache file $cacheFile, $e")
                }
            }
            resp
        },
        isValid = { resp ->
            resp?.status == 200 && resp.timeStamp > Instant.now(Clock.systemUTC()).epochSecond + cachePeriod.inWholeSeconds
        }
    )

}
