// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.apiPlugins.SfcConfigSchema
import com.amazonaws.sfc.apiPlugins.sfcApiApp
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.lang.Integer.parseInt
import java.security.PublicKey
import java.sql.Connection
import java.sql.DriverManager

@ConfigurationClass
class CustomApiUiConfigProvider(private val configStr: String, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    private val initCfg = Json.parseToJsonElement(configStr).jsonObject
    // create dummy start config from file (in order that SFC receives a valid conf at fresh start)
    private val dummyStartCfg = this::class.java.classLoader.getResource("initialDummyConfig.json")?.readText()

    // extract Custom LogWriter JSON-Object from start config - in order to receive websockets in the UI
    private val writerObj = Json.parseToJsonElement(dummyStartCfg.toString()).jsonObject.get("LogWriter")

    // extract ConfigProvider Obj from start cfg
    private val confProviderObj = Json.parseToJsonElement(initCfg.toString()).jsonObject.get("ConfigProvider")

    init {
        // CHECK IF VALID CONF EXISTS IN DB - IF YES -> SEND CFG TO CORE
        runBlocking{
            if (writerObj != null && confProviderObj != null) {
                checkConf(ch, dummyStartCfg.toString(), writerObj, confProviderObj, initCfg, logger)
            }
        }
    }

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
            //pass dummy start conf & logWriter Object to APIApp - writerObj is attached always when Config is created via API
            embeddedServer(Tomcat, port = getPort(initCfg, logger)) {
                    if (writerObj != null && confProviderObj != null) {
                        sfcApiApp(ch, logger, writerObj, confProviderObj)
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

        fun getPort(initialCfg: JsonObject, log: Logger): Int {
            return if(initialCfg.getValue("ConfigProvider").jsonObject.containsKey("Port")) {
                try {
                    val port = parseInt(initialCfg["ConfigProvider"]?.jsonObject?.get("Port").toString())
                    log.info("Using port $port from Config file","")
                    port
                } catch (e: Exception) {
                    8080
                }
            } else {
                8080
            }
        }

        suspend fun checkConf(ch: Channel<String>, dummy: String, writer: JsonElement, confProvider: JsonElement, initialCfg: JsonObject, log: Logger) {
            // get SelectedConfIdFromDB from start-cfg -> the config having that ID from DB will be loaded
            val connection: Connection = checkDBconn()
            val configService = SfcConfigSchema(connection)
            // prepare dummy config and include conf-provider sections
            var dummyJson = Json.parseToJsonElement(dummy).jsonObject
            dummyJson = JsonObject(dummyJson + ("ConfigProvider" to confProvider))

            if (initialCfg.getValue("ConfigProvider").jsonObject.containsKey("SelectedConfIdFromDB")) {

                val initId = initialCfg["ConfigProvider"]?.jsonObject?.get("SelectedConfIdFromDB").toString()
                // get conf from DB as String
                try {
                    val conf = configService.read(initId.toInt())
                    // create JSON
                    var confJson = Json.parseToJsonElement(conf).jsonObject
                    // attach our LogWriter Obj to receive logs as Websockets && attach ConfigProvider Obj
                    if (!confJson.containsKey("LogWriter")) {
                        confJson = JsonObject(confJson + ("LogWriter" to writer) + ("ConfigProvider" to confProvider))
                    }

                    log.info("Pinned SFC Config found in DB: ID=$initId - sending to SFC-CORE", "")
                    log.info(conf,"")

                    ch.send(Json.encodeToString(confJson))
                } catch (e: Exception) {
                    log.error(e.localizedMessage, "")
                    ch.send(Json.encodeToString(dummyJson))
                    log.info("No pinned SFC Config found in DB! pls check `SelectedConfIdFromDB` in config file - sending dummy cfg...","")
                }
            } else {
                ch.send(Json.encodeToString(dummyJson))
                log.info("No pinned SFC Config ID set in config key SelectedConfIdFromDB - sending dummy cfg...","")
            }
        }


        private fun checkDBconn(): Connection {
            Class.forName("org.h2.Driver")
            return DriverManager.getConnection("jdbc:h2:file:./sfctest;DB_CLOSE_DELAY=-1", "root", "")
        }

        /*@JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            // start HTTP API
            val testChannel = Channel<String>(1)
            val log = Logger.createLogger()
            val dummyStartCfg = this::class.java.classLoader.getResource("initialDummyConfig.json")?.readText()
            val writerObj = Json.parseToJsonElement(dummyStartCfg.toString()).jsonObject.get("LogWriter")
            //print(dummyStartCfg)

            embeddedServer(Tomcat, port = 8080){
                if (writerObj != null) {
                    sfcApiApp(testChannel, log, writerObj)
                }
            }.start(wait = true)
        }*/
    }
}
