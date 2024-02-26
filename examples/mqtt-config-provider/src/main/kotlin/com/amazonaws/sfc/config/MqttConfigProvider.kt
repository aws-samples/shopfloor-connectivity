// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import MqttConfigProviderConfig
import com.amazonaws.sfc.mqtt.MqttHelper
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.PublicKey

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

    private val mqttClient by lazy {
        val mqttHelper = MqttHelper(providerConfig, logger)
        mqttHelper.buildClient()
    }


    val worker = scope.launch {

        val channel = Channel<String>()
        val log = logger.getCtxLoggers(className, "mqtt config provider")

        val c = mqttClient
        println(c)

        c.subscribe(providerConfig.topicName) { _, message ->
            println(message.payload.toString())
            runBlocking {
                ch.send(String(message.payload))
            }
        }

        while (isActive) {
            val message = channel.receive()
            println("Processing $message")

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


