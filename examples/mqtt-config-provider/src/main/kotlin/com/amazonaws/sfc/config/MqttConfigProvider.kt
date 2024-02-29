// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.data.JsonHelper.Companion.gsonPretty
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.mqtt.MqttHelper
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.getHostName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.security.PublicKey
import kotlin.coroutines.CoroutineContext

@ConfigurationClass
class MqttConfigProvider(
    private val configStr: String,
    private val configVerificationKey: PublicKey?,
    private val logger: Logger
) : ConfigProvider {

    private val className = this::class.java.simpleName

    // channel used to send configurations to SFC-Core
    private val ch = Channel<String>(1)

    private val scope = buildScope("MqttConfigProvider")

    // Get initial JSON config, this config contains the information to connect and subscribe to the topic and a reference to a stored local configuration file
    private val providerConfig: MqttConfigProviderConfig by lazy {
        ConfigReader.createConfigReader(configStr).getConfig(validate = true)
    }

    private var _mqttClient: MqttClient? = null
    private suspend fun getClient(context: CoroutineContext): MqttClient {

        val log = logger.getCtxLoggers(className, "getClient")

        while (_mqttClient == null && context.isActive) {
            try {
                val mqttHelper = MqttHelper(providerConfig, logger)
                _mqttClient = mqttHelper.buildClient("sfc_config_provider_${getHostName()}")
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "mqttClient")("Error creating and connecting mqttClient. $e")
            }
            if (_mqttClient == null) {
                log.info("Waiting ${providerConfig.waitAfterConnectError} before trying to create and connecting MQTT client")
                delay(providerConfig.waitAfterConnectError)
            }
        }
        return _mqttClient as MqttClient
    }


    private fun getUrl(s: String): URL? = try {
        val url = URL(s)
        url.toURI()
        url
    } catch (e: Exception) {
        // not a valid url
        null
    }


    private fun downloadFile(url: URL): String {

        val connection: URLConnection = url.openConnection()

        connection.connect()

        val reader = BufferedReader(InputStreamReader(connection.inputStream))

        val response = StringBuilder()
        var inputLine: String?
        while (reader.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        reader.close()

        return response.toString()
    }

    private fun downLoadConfiguration(message: ByteArray): String? {

        val log = logger.getCtxLoggers(className, "downLoadConfig")
        val s = String(message)
        val url = getUrl(s)
        return if (url != null) {
            log.info("Downloading configuration from from $url")
            try {
                val configString = downloadFile(url)
                log.info("Configuration downloaded, size is ${configString}")
                log.trace(configString)
                return downloadFile(url)
            } catch (e: Exception) {
                log.error("Error downloading configuration from $url, ${e.message}")
                throw e

            }
        } else null
    }


    val worker = scope.launch {

        val channel = Channel<String>()
        val log = logger.getCtxLoggers(className, "mqtt config provider")
        var loadedLocalFile = false

        if ( providerConfig.useLocalConfigFileAtStartUp && providerConfig.localConfigFile?.exists() == true) {
            log.info("Loading existing local configuration file ${providerConfig.localConfigFile!!.absolutePath}")
            try {
                val configStr = providerConfig.localConfigFile!!.readText()
                loadedLocalFile = emitConfiguration(configStr)
            } catch (e: Exception) {
                log.error("Error loading configuration from local file, ${e.message}")
            }
        } else{
            log.info("Local configuration file ${providerConfig.localConfigFile!!.absolutePath} does not exist")
        }


        val client = getClient(coroutineContext)

        if (!loadedLocalFile) {
            log.info("No configuration from local configuration file, waiting for configuration from broker  ${providerConfig.endPoint} topic ${providerConfig.topicName}")
        }

        log.info("Connected to ${providerConfig.endPoint}, subscribing to topic ${providerConfig.topicName}")

        client.subscribe(providerConfig.topicName) { _, message ->
            log.info("Received configuration from topic ${providerConfig.topicName}")
            val configStr = downLoadConfiguration(message.payload) ?: String(message.payload)
            emitConfiguration(configStr)
        }

        while (isActive) {
            try {
                ch.send(channel.receive())
            } catch (e: Exception) {
                log.error("Error sending configuration to SFC-Core, ${e.message.toString()}")
                break
            }
        }
    }

    private fun emitConfiguration(configStr: String) : Boolean{

        val log = logger.getCtxLoggers(className, "validateAndEmitConfiguration")
        return try {
            val config = ConfigReader.createConfigReader(configStr).getConfig<ControllerServiceConfiguration>()
            runBlocking {
                writeConfigToLocalFile(config)
                log.info("Sending configuration to SFC-Core")
                ch.send(configStr)
            }
            true
        } catch (e: Exception) {
            log.error("Invalid configuration, ${e.message.toString()}")
            false
        }
    }

    private fun writeConfigToLocalFile(config: ControllerServiceConfiguration) {
        val log = logger.getCtxLoggers(className, "writeConfigToLocalFile")


        try {
            val localFile = providerConfig.localConfigFile
            if (localFile != null) {
                if (!localFile.exists()) {
                    localFile.createNewFile()
                }
                log.info("Writing configuration to local file ${providerConfig.localConfigFile}")
                localFile.writeText(gsonPretty().toJson(config))
            }
        } catch (e: Exception) {
            log.error("Error writing configuration to local file, ${e.message.toString()}")
        }
    }


    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return MqttConfigProvider(
                createParameters[0] as String,
                createParameters[1] as PublicKey?,
                createParameters[2] as Logger
            )
        }
    }
}


