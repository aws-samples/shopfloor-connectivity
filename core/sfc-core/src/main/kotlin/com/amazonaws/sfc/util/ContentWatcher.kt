// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

class ContentWatcher(private val url: Url, private val interval: Duration) {

    inner class ContentUpdate(val url: Url, val crc: Long){
        override fun toString(): String {
            return "(url=$url, crc=$crc)"
        }
    }

    private val scope = buildScope("ContentWatcher")

    private var contentCrc: Long?
    val ch = Channel<Long>()

    init {
        contentCrc = runBlocking {
            getCrc()
        }
    }

    val watcher = scope.launch {
        while(isActive) {
            delay(interval)
            val newCrc = getCrc()
            if (newCrc != null && newCrc != contentCrc) {
                ch.send(newCrc)
            }
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun getCrc(): Long? {
        val client = HttpClient(CIO)
        try {
            val resp = client.get(url)
            if (resp.status != HttpStatusCode.OK) return null
            val content = resp.content.toByteArray()
            return crc32(content)
        } catch (e: Exception) {
            println(e)
            return null
        }
    }

    val changes = flow {
        for (crc in ch) {
            contentCrc = crc
            emit(ContentUpdate(url, crc))
        }
    }


    // using this instead of CRC32 for jvm language level 8
    private fun crc32(bytes: ByteArray): Long {
        var crc = 0L
        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and 0xFF).toLong())
            for (i in 0..7) {
                crc = if ((crc and 1) == 1L) {
                    (crc shr 1) xor 0xEDB88320L
                } else {
                    crc shr 1
                }
            }
        }
        return crc
    }
}
