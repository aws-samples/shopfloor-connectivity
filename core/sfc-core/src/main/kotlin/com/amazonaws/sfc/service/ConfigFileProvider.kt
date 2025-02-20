
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_MONITOR_INCLUDED_CONFIG_CONTENT_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_MONITOR_INCLUDED_CONFIG_FILES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.DEFAULT_MONITOR_INCLUDED_CONFIG_CONTENT_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.DEFAULT_MONITOR_INCLUDED_CONFIG_FILES
import com.amazonaws.sfc.config.InProcessConfiguration.Companion.getCustomConfig
import com.amazonaws.sfc.config.IncludeResolver.IncludeResolverException
import com.amazonaws.sfc.config.IncludeResolver.urlRegex
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.ContentWatcher
import com.amazonaws.sfc.util.FileWatcher
import com.amazonaws.sfc.util.buildScope
import com.google.gson.JsonSyntaxException
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.File
import java.security.PublicKey
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * Configuration provider for file based configuration.
 */
class ConfigFileProvider(private val configFile: File, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val className = this::class.java.simpleName

    private var configMutex = ReentrantReadWriteLock()
    private val configChannel = Channel<String>()

    val scope = buildScope("Configuration File Config Provider", Dispatchers.IO)
    private var lastSubmittedConfig: String? = null
    private val includedFileWatchers = mutableMapOf<String, Job>()
    private val includedContentWatchers = mutableMapOf<String, Job>()

    private fun customConfigProvider(configStr: String, configVerificationKey: PublicKey?): ConfigProvider? {
        val log = logger.getCtxLoggers(className, "customConfigProvider")
        try {

            val customConfigProviderConfig = getCustomConfig(configStr, CONFIG_CUSTOM_CONFIG_PROVIDER) ?: return null

            // Create factory and new instance of custom provider
            val factory = CustomConfigurationProviderFactory(customConfigProviderConfig, configVerificationKey, logger)
            return factory.newProviderInstance(configStr, logger)

        } catch (e: JsonSyntaxException) {
            val msg = "Error creating custom configuration provider, invalid JSON syntax in configuration"
            log.error("msg, $e")
            throw JsonSyntaxException(msg)
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
            } catch (e: Exception) {
                loggers.errorEx("Error in configuration provider watch file", e)
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                watchEnvironment()
            } catch (e: Exception) {
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
                                handleNewConfiguration(config, ch)
                            }
                        }
                        customProvider?.configuration?.onReceive { customConfig ->
                            loggers.trace("Emitting custom configuration")
                            handleNewConfiguration(customConfig, ch)
                        }
                    }
                } catch (e: Exception) {
                    if (e is IncludeResolverException)
                        loggers.error("Error resolving configuration, $e")
                    else
                        loggers.error("Error in configuration , $e")
                }
            }
        }
        ch
    }

    private fun getIncludeWatcherSettings(config : String): Pair<Boolean, Duration>{
        @Suppress("UNCHECKED_CAST")
        val map = fromJsonExtended(config, Any::class.java) as Map<String, Any?>
        val watchFiles = (map[CONFIG_MONITOR_INCLUDED_CONFIG_FILES]?: DEFAULT_MONITOR_INCLUDED_CONFIG_FILES) as Boolean
        val interval = ((map[CONFIG_MONITOR_INCLUDED_CONFIG_CONTENT_INTERVAL]?: DEFAULT_MONITOR_INCLUDED_CONFIG_CONTENT_INTERVAL)as Int).toDuration(DurationUnit.SECONDS)
        return Pair(watchFiles, interval)
    }

    private suspend fun handleNewConfiguration(config: String, ch: Channel<String>) {
        val includedItems = ConfigReader.getIncludedItems(config)
        if (includedItems.isNotEmpty()) {
            val (checkFiles, checkContentInterval) = getIncludeWatcherSettings(config)
            if (checkFiles) setupIncludedFileWatchers(includedItems)
            if (checkContentInterval != 0.toDuration(DurationUnit.SECONDS)) setupIncludedContentWatchers(includedItems, checkContentInterval)
        }
        lastSubmittedConfig = config
        ch.send(config)
    }


    private suspend fun createIncludedFileWatcher(s: String): Job {
        return scope.launch(Dispatchers.IO) {
            val watcher = FileWatcher(File(s))
            watcher.changes.collect { changedFile ->
                val log = logger.getCtxLoggers(className, "includedFileWatcher:${changedFile.entry}")
                log.info("Included configuration file \"${changedFile.entry}\" changed")
                IncludeResolver.removeFromCache(File(changedFile.entry))
                configuration?.send(lastSubmittedConfig!!)
            }
        }
    }

    private suspend fun createIncludedContentWatcher(s: String, checkContentInterval: Duration): Job {
        return scope.launch(Dispatchers.IO) {
            if (checkContentInterval == 0.toDuration(DurationUnit.SECONDS) ) return@launch
            val watcher = ContentWatcher(Url(s), checkContentInterval)
            watcher.changes.collect { changedContent ->
                val log = logger.getCtxLoggers(className, "includedContentWatcher:${changedContent.url}")
                log.info("Included content from url \"${changedContent.url}\" changed")
                IncludeResolver.removeFromCache(changedContent.url)
                configuration?.send(lastSubmittedConfig!!)
            }
        }
    }

    private suspend fun setupIncludedFileWatchers(includedItems: List<String>) {

        val log = logger.getCtxLoggers(className, "setupIncludedFileWatchers")
        val includedFiles = includedItems.filter { it.startsWith("file:") }.map { it.substring(5) }
        if (includedFiles.isNotEmpty()) log.trace("Included files: $includedFiles")

        includedFileWatchers.filter { !includedFiles.contains(it.key) }.forEach { (f, w) ->
            log.trace("Stopping watcher for file $f")
            w.cancel()
            includedFileWatchers.remove(f)
        }

        includedFiles.filter { !includedFileWatchers.containsKey(it) }.forEach {
            log.trace("Starting watcher for configuration include file $it")
            includedFileWatchers[it] = createIncludedFileWatcher(it)

        }
    }

    private suspend fun setupIncludedContentWatchers(includedItems: List<String>, checkContentInterval: Duration) {

        val log = logger.getCtxLoggers(className, "setupIncludedContentWatchers")
        val includedUrls = includedItems.filter { urlRegex.matches(it) }
        if (includedUrls.isNotEmpty()) log.trace("Included urls: $includedUrls")

        includedContentWatchers.filter { !includedUrls.contains(it.key) }.forEach { (u, w) ->
            log.trace("Stopping watcher for url $u")
            w.cancel()
            includedContentWatchers.remove(u)
        }

        includedUrls.filter { !includedContentWatchers.containsKey(it) }.forEach {
            log.trace("Starting watcher for configuration include url $it")
            includedContentWatchers[it] = createIncludedContentWatcher(it, checkContentInterval)

        }
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

