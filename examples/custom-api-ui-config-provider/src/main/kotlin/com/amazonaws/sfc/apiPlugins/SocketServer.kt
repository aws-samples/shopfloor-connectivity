// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

import com.amazonaws.sfc.log.Logger
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.EOFException
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureSockets(log: Logger) {

    val className = this.javaClass::getName.name
    val loggers = log.getCtxLoggers(className, "configureSockets")

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        val connections = Collections.synchronizedSet<SocketConn?>(LinkedHashSet())
        webSocket("/logreceiver") {
            loggers.info("Adding websocket client!")
            val thisConnection = SocketConn(this)
            val connCount = connections.size
            if (connCount > 6) {
                loggers.info("Clearing $connCount websocket sessions!")
                connections.clear()
            }
            connections += thisConnection
            try {
                send("There are ${connections.count()} websocket clients here...")
                for (frame in incoming) {
                    frame as Frame.Text
                    val receivedText = frame.readText()
                    connections.forEach {
                        it.session.send(receivedText)
                    }
                }
            } catch(eof: EOFException){
                loggers.warningEx(eof.localizedMessage, eof)
            } catch (e: Exception) {
                loggers.errorEx(e.localizedMessage, e)
            } finally {
                loggers.info("Removing websocket client $thisConnection!")
                connections -= thisConnection
            }
        }
    }

}