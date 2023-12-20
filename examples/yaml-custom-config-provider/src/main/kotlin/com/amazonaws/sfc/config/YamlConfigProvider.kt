// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.FileWatcher
import com.amazonaws.sfc.util.buildScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.PublicKey

@ConfigurationClass
class YamlConfigProvider(
    private val configStr: String,
    private val configVerificationKey: PublicKey?,
    private val logger: Logger
) : ConfigProvider {

    private val className = this::class.java.simpleName

    // channel used to send configurations to SFC-Core
    private val ch = Channel<String>(1)
    
    private val scope = buildScope("YamlConfigProvider")

    // Read minimal JSON config, this config only has reference to this custom YAML config handler and the name of the actual YAML config file
    private val config: YamlConfigProviderConfig by lazy {
        ConfigReader.createConfigReader(configStr).getConfig(validate = true)
    }

    // Gets the YAML from the config file and converts it to JSON
    private val configFromYamlAsJson: String?
        get() {
            val log = logger.getCtxLoggers(className, "convertYamlToJson")

            log.info("Converting YAML from ${config.yamlConfigFile?.absolutePath} to JSON")
            return try {
                val yamlReader = ObjectMapper(YAMLFactory())
                val yaml = yamlReader.readValue(config.yamlConfigFile, Any::class.java)
                val jsonWriter = ObjectMapper()
                return jsonWriter.writeValueAsString(yaml)
            } catch (e: Exception) {
                log.error("Error converting YAML from ${config.yamlConfigFile?.absolutePath} to JSON, $e")
                null
            }
        }

    val worker = scope.launch {

        val log = logger.getCtxLoggers(className, "watchYamlConfigFile")

        val jsonConfig = configFromYamlAsJson

        if (jsonConfig != null) {
            log.info("Sending initial configuration to SFC-Core")
            log.trace("Configuration: $jsonConfig")
            ch.send(jsonConfig)
        } else{
            log.info("Waiting for updated valid config")
        }

        val fileWatcher = FileWatcher(config.yamlConfigFile!!)

        while (isActive) {

            // watch yaml config file for updates
            fileWatcher.changes.collect {
                // Send to core if it could be converted to JSON
                log.info("YAML config file updated")
                val configFromYamlAsJson = configFromYamlAsJson

                if (configFromYamlAsJson != null) {
                    log.info("Sending updated configuration to SFC-Core")
                    ch.send(configFromYamlAsJson)
                }
            }
        }
    }


    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return YamlConfigProvider(
                createParameters[0] as String,
                createParameters[1] as PublicKey?,
                createParameters[2] as Logger
            )
        }
    }
}
