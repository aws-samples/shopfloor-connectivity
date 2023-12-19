// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.log

import com.amazonaws.sfc.apiPlugins.SocketMessage
import com.amazonaws.sfc.util.buildScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.cio.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.EOFException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class SocketLogWriter(private val configStr: String) : LogWriter, SocketMessage {

    private val log: Logger = Logger.createLogger()
    private val scope = buildScope("SocketLogWriter")

    private val initCfg = Json.parseToJsonElement(configStr).jsonObject


    override fun write(logLevel: LogLevel, timestamp: Long, source: String?, message: String) {
        val dtm = "%-23s".format(SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SS").format(Date()))
        val sourceStr = if (source != null) "[$source] :" else ""
        // Co-Routine
        scope.launch { sendMessage("$dtm $logLevel- $sourceStr $message") }
    }

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 15.seconds.inWholeMilliseconds
        }
    }

    private val session = runBlocking {
        println(initCfg)
        client.webSocketRawSession(method = HttpMethod.Get,
            host = "localhost",
            port = getPort(initCfg, log),
            path = "/logreceiver")
    }

    override suspend fun sendMessage(message: String) {
        try{
            session.outgoing.send(Frame.Text(message))
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Pong -> {

                        log.trace("ping's response received","")
                    }
                    is Frame.Ping -> {
                        session.outgoing.send(Frame.Pong(frame.buffer))
                        log.trace("ping recieved from ${frame.javaClass}","")
                    }
                    is Frame.Text -> {
                        //do nothing here
                    }
                    is Frame.Binary -> TODO()
                    is Frame.Close -> TODO()
                }
            }
        } catch(eof: EOFException){
            log.warning(eof.localizedMessage, this::class.java.name)
        }catch (e: Exception){
            // do nothing
            // log.error(e.localizedMessage, this.javaClass::getName.name)
        }
    }

    override suspend fun receiveMessage(message: (String) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): SocketLogWriter {
            return SocketLogWriter(createParameters[0] as String)
        }

        fun getPort(initialCfg: JsonObject, log: Logger): Int {
            return if(initialCfg.getValue("ConfigProvider").jsonObject.containsKey("Port")) {
                try {
                    val port = Integer.parseInt(initialCfg["ConfigProvider"]?.jsonObject?.get("Port").toString())
                    log.info("Using port $port from Config file","")
                    port
                } catch (e: Exception) {
                    8080
                }
            } else {
                8080
            }
        }
    }
}