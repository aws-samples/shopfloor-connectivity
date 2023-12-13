// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.apiPlugins.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.security.PublicKey

@ConfigurationClass
class CustomApiUiConfigProvider(private val configStr: String, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    // this code could for example call out to external sources and combine retrieved information with
    // data from the passed in configuration
    private val dummyStartCfg = this::class.java.classLoader.getResource("initialDummyConfig.json")?.readText()
    val writerObj = Json.parseToJsonElement(dummyStartCfg.toString()).jsonObject.get("LogWriter")


    val worker = scope.launch {
        val errorLog = logger.getCtxErrorLog(this::class.java.name, "worker")
        while (true) {
            if (configVerificationKey != null) {
                if (!ConfigVerification.verify(configStr, configVerificationKey)) {
                    errorLog("Content of configuration could not be verified")
                    continue
                }
            }
            // inject custom LogWriter to config
            //print(writerObj)
            ch.send(dummyStartCfg.toString())

            // start HTTP API
            embeddedServer(Tomcat, port = 8080, host = "0.0.0.0"){
                if (writerObj != null) {
                    sfcApiApp(ch, logger, writerObj)
                }
            }.start(wait = true)
        }
    }

    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return CustomApiUiConfigProvider(createParameters[0] as String, createParameters[1] as PublicKey?, createParameters[2] as Logger)
        }

        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            // start HTTP API
            val testChannel = Channel<String>(1)
            val log = Logger.createLogger()
            val dummyStartCfg = this::class.java.classLoader.getResource("initialDummyConfig.json")?.readText()
            val writerObj = Json.parseToJsonElement(dummyStartCfg.toString()).jsonObject.get("LogWriter")
            //print(dummyStartCfg)

            embeddedServer(Tomcat, port = 8080, host = "0.0.0.0"){
                if (writerObj != null) {
                    sfcApiApp(testChannel, log, writerObj)
                }
            }.start(wait = true)
        }
        }
    }
