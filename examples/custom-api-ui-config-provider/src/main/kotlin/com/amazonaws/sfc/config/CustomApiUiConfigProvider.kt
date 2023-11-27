
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.apiPlugins.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.tomcat.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.security.PublicKey
import java.sql.Connection


@ConfigurationClass
class CustomApiUiConfigProvider(private val configStr: String, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    // this code could for example call out to external sources and combine retrieved information with
    // data from the passed in configuration
    val worker = scope.launch {
        val errorLog = logger.getCtxErrorLog(this::class.java.name, "worker")
        while (true) {
            if (configVerificationKey != null) {
                if (!ConfigVerification.verify(configStr, configVerificationKey)) {
                    errorLog("Content of configuration could not be verified")
                    continue
                }
            }
            // start HTTP API
            embeddedServer(Tomcat, port = 8080, host = "0.0.0.0"){
                sfcApiApp()
            }.start(wait = true)
        }
    }

    private fun Application.sfcApiApp() {
        configureSerialization()
        configureRouting()
        val dbConnection: Connection = connectToPostgres(embedded = true)
        val configService = SfcConfigService(dbConnection)
        routing {
            // Create config
            post("/config") {
                val config = call.receive<SfcConfig>()
                val id = configService.create(config)
                call.respond(HttpStatusCode.Created, id)
            }

            // Push existing config to SFC-MAIN
            post("/push/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                try {
                    val config = configService.read(id)
                    // send config to SFC-MAIN
                    ch.send(config)
                    call.respond(HttpStatusCode.Created, config)
                } catch (e: Exception) {
                    print(e.stackTrace)
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Read config
            get("/config/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                try {
                    val config = configService.read(id)
                    call.respond(HttpStatusCode.OK, config)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // Update config
            put("/config/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val user = call.receive<SfcConfig>()
                configService.update(id, user)
                call.respond(HttpStatusCode.OK)
            }
            // Delete city
            delete("/config/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                configService.delete(id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return CustomApiUiConfigProvider(createParameters[0] as String, createParameters[1] as PublicKey?, createParameters[2] as Logger)
        }

    }

}