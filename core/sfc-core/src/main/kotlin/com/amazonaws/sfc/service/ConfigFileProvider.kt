
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigVerification
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.CustomConfigurationProviderFactory
import com.amazonaws.sfc.config.InProcessConfiguration.Companion.getCustomConfig
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.FileWatcher
import com.amazonaws.sfc.util.buildScope
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.File
import java.security.PublicKey
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


/**
 * Configuration provider for file based configuration.
 */
class ConfigFileProvider(private val configFile: File, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val className = this::class.java.simpleName

    private var configMutex = ReentrantReadWriteLock()
    private val configChannel = Channel<String>()

    val scope = buildScope("Configuration File Config Provider")

    private fun customConfigProvider(configStr: String, configVerificationKey: PublicKey?): ConfigProvider? {
        val log = logger.getCtxLoggers(className, "customConfigProvider")
        try {

            val customConfigProviderConfig = getCustomConfig(configStr, CONFIG_CUSTOM_CONFIG_PROVIDER) ?: return null

            // Create factory and new instance of custom provider
            val factory = CustomConfigurationProviderFactory(customConfigProviderConfig, configVerificationKey, logger)
            return factory.newProviderInstance(configStr, logger)

        } catch( e : JsonSyntaxException){
             val msg = "Error creating custom configuration provider, invalid JSON syntax in configuration"
             log.errorEx(msg, e.extendedJsonException(configStr))
             throw ConfigurationException(msg, CONFIG_CUSTOM_CONFIG_PROVIDER)
        } catch (e: Exception) {
            val msg = "Error creating custom configuration provider"
            log.errorEx(msg, e)
            throw ConfigurationException(msg, CONFIG_CUSTOM_CONFIG_PROVIDER)
        }
    }


    // last configuration data
    private var _lastConfig: String? = null
    private var lastConfig: String?
        get() =
            configMutex.read {
                _lastConfig
            }
        set(value) {
            configMutex.write {
                _lastConfig = value
                _lastEnvironment = null
            }
        }

    private var _lastEnvironment: Map<String, String?>? = null
    private var lastEnvironment: Map<String, String?>?
        get() = configMutex.read {
            _lastEnvironment
        }
        set(value) {
            configMutex.write {
                _lastEnvironment = value
            }
        }

    override val configuration: Channel<String>? by lazy {

        val ch = Channel<String>()

        var customProvider: ConfigProvider? = null

        val loggers = logger.getCtxLoggers(className, "environmentWatcher")
        if (!configFile.exists()) {
            loggers.error("Configuration file ${configFile.absoluteFile} does not exist")
            return@lazy null
        }

        scope.launch(Dispatchers.IO) {
            try {
                watchFile()
            }catch (e : Exception){
                loggers.errorEx("Error in configuration provider watch file", e)
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                watchEnvironment()
            }catch (e : Exception){
                loggers.errorEx("Error in configuration provider watch environment", e)
            }
        }

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    select {
                        configChannel.onReceive { config ->
                            customProvider = customConfigProvider(config, configVerificationKey)
                            if (customProvider == null) {
                                loggers.trace("Emitting configuration")
                                ch.send(config)
                            }
                        }
                        customProvider?.configuration?.onReceive { customConfig ->
                            loggers.trace("Emitting custom configuration")
                            ch.send(customConfig)
                        }
                    }
                }catch (e : Exception){
                    loggers.errorEx("Error in configuration provider", e)
                }
            }
        }
        ch
    }


    private suspend fun watchFile() {
        val loggers = logger.getCtxLoggers(className, "fileWatcher")
        val watcher = FileWatcher(configFile)

        lastConfig = configFile.readText()
        if (verifyConfig(lastConfig!!)) {
            loggers.info("Sending initial configuration from file \"${configFile.name}\"")
            if (lastConfig != null) {
                configChannel.send(lastConfig!!)
            }
        }

        watcher.changes.collect {
            val config = configFile.readText()
            if (config != lastConfig) {
                if (verifyConfig(config)) {
                    loggers.info("Updating configuration from file \"${configFile.name}\"")
                    configChannel.send(config)
                }
            }
            lastConfig = config
        }
    }

    private fun verifyConfig(configStr: String): Boolean {
        val loggers = logger.getCtxLoggers(className, "verifyConfig")
        if (configVerificationKey == null) return true

        if (!ConfigVerification.verify(configStr, configVerificationKey)) {
            loggers.error("Content of config file could not be verified")
            return false
        }
        return true
    }

    private suspend fun watchEnvironment() {
        val loggers = logger.getCtxLoggers(className, "environmentWatcher")

        while (true) {
            delay(5000)
            if (lastConfig != null) {
                val environment = environmentVariables(lastConfig ?: "")
                if (lastEnvironment == null) {
                    lastEnvironment = environment
                    continue
                }
                if (environment != lastEnvironment) {
                    loggers.info("Updating configuration from as environment variable(s) have changed")
                    configChannel.send(lastConfig!!)
                    lastEnvironment = environment
                }
            }
        }
    }


    private fun environmentVariableNames(s: String?) =
        if (s == null) {
            emptySet()
        } else {
            ConfigReader.getExternalPlaceHolders(s)
        }


    private fun environmentVariables(s: String) =
        environmentVariableNames(s).associateWith { variableName ->
            System.getenv(variableName)
        }

    companion object {
        const val CONFIG_CUSTOM_CONFIG_PROVIDER = "ConfigProvider"
    }
}

