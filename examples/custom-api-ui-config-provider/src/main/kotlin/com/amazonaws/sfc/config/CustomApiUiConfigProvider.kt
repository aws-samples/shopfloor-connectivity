// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.apiPlugins.SfcConfig
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
import java.net.InetAddress
import java.security.PublicKey
import java.sql.DriverManager
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*

@ConfigurationClass
class CustomApiUiConfigProvider(private val configStr: String, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    private val initCfg = Json.parseToJsonElement(configStr).jsonObject

    // extract Custom LogWriter JSON-Object from start config - in order to receive websockets in the UI
    private val writerObj = Json.parseToJsonElement(initCfg.toString()).jsonObject.get("LogWriter")

    // extract ConfigProvider Obj from start cfg
    private val confProviderObj = Json.parseToJsonElement(initCfg.toString()).jsonObject.get("ConfigProvider")

    init {
        runBlocking{
            if (writerObj != null && confProviderObj != null) {
                checkConf(ch, writerObj, confProviderObj, initCfg, logger)
            }
            else {
                logger.warning("Make sure to include CustomConfigProvider and LogWriter configs in order to use the UI & API config provider","")
                //logger.info(Json.encodeToString(initCfg),"")
                ch.send(Json.encodeToString(initCfg))
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

        suspend fun checkConf(ch: Channel<String>, writer: JsonElement, confProvider: JsonElement, initialCfg: JsonObject, log: Logger) {
            try {
                // attach our LogWriter Obj to receive logs as Websockets && attach ConfigProvider Obj
                var confJson = initialCfg

                confJson = JsonObject(confJson + ("LogWriter" to writer) + ("ConfigProvider" to confProvider))

                // store initial cfg to DB
                val connection: Connection = checkDBconn()
                val configService = SfcConfigSchema(connection)
                val dtm = "%-23s".format(SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date()))
                val host = getSystemName()
                val newSfcConfig = SfcConfig("StartCfg@$host-$dtm", confJson)

                val newCfgId: Int = configService.create(newSfcConfig)
                // mark initial cfg as pushed in DB
                configService.push(newCfgId)

                //log.info(Json.encodeToString(confJson),"")

                ch.send(Json.encodeToString(confJson))
            } catch (e: Exception) {
                log.error(e.localizedMessage, "")
            }
        }

        private fun checkDBconn(): Connection {
            Class.forName("org.h2.Driver")
            return DriverManager.getConnection("jdbc:h2:file:./sfctest;DB_CLOSE_DELAY=-1", "root", "")
        }

        private fun getSystemName(): String? {
            return try {
                InetAddress.getLocalHost().hostName
            } catch (e: Exception) {
                null
            }
        }
    }
}
