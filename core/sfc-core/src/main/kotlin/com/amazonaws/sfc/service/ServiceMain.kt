
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service


import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.CustomLogWriterFactory
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.LogWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.log.Logger.Companion.createLogger
import com.amazonaws.sfc.util.MemoryMonitor
import com.amazonaws.sfc.util.launch
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Main class for running a service.
 * This class provides consistent handling of input parameters, logging etc.
 * Inherited classes must overwrite the createServiceInstance method to provide the actual service to run
 * This class provides consistent handling of handling configuration, logging, command line arguments etc. for all services
 */
abstract class ServiceMain {

    private val className = this::class.java.simpleName

    private var serviceInstance: Service? = null

    private var serviceLogger = createLogger()

    private var customWriter: LogWriter? = null
    private fun customLogWriter(configStr: String): LogWriter? {
        try {

            val customConfigProviderConfig = InProcessConfiguration.getCustomConfig(configStr, Logger.CONF_LOG_WRITER) ?: return null

            // Create factory and new instance of custom provider
            val factory = CustomLogWriterFactory(customConfigProviderConfig, createLogger(configStr))
            return factory.newLogWriterInstance(configStr)
        } catch( e : JsonSyntaxException){
            val msg = "Error creating custom log writer, invalid JSON in configuration ${e.extendedJsonException(configStr)}"
            throw ConfigurationException(msg, Logger.CONF_LOG_WRITER)
        } catch (e: Exception) {
            val msg = "Error creating custom log writer, $e"
            throw ConfigurationException(msg, Logger.CONF_LOG_WRITER)
        }
    }


    /**
     * Runs the service as an application
     * @param args Array<String> Command line arguments
     */
    open suspend fun run(args: Array<String>): Unit = coroutineScope {

        // as config provider is not initiated here check if loglevel was provided on command line to
        // enable specification of the loglevel used by the configuration provider itself
        val logLevelFromArguments = LogLevel.fromArgs(args)
        if (logLevelFromArguments != null) {
            serviceLogger.level = logLevelFromArguments
        }

        Logger.redirectLoggers(serviceLogger, className)

        val logs = serviceLogger.getCtxLoggers(className, "run")

        var memoryMonitor : MemoryMonitor? = null

        try {
            val configProvider = ConfigProviderFactory.createProvider(args, serviceLogger)

            if (configProvider == null) {
                memoryMonitor = MemoryMonitor(scope = this, logger = serviceLogger)
                createAndRunServiceInstance(args, "{}")
            } else {
                while (isActive && configProvider.configuration != null) {
                    logs.info("Waiting for configuration")
                    val configuration = configProvider.configuration?.receive() ?: continue
                    serviceLogger = createLogger(configuration)
                    memoryMonitor?.stop()
                    memoryMonitor = MemoryMonitor(scope = this, logger = serviceLogger)
                    Logger.redirectLoggers(serviceLogger, className)
                    handleCustomLogWriter(configuration)
                    logs.info("Received configuration data from config provider")
                    stopService()
                    launch(context = Dispatchers.IO, name = "Create Service Instance") {
                        try {
                            logs.info("Creating and starting new service instance")
                            createAndRunServiceInstance(args, configuration)
                        } catch (e: Exception) {
                            logs.errorEx("Error creating service instance", e)
                        }
                    }
                }
            }
        }catch (e : Exception){
            logs.errorEx("Error running service", e)
        }
        finally {
            memoryMonitor?.stop()
        }
    }

    private fun handleCustomLogWriter(configStr: String) {
        val writer = customLogWriter(configStr)
        if (writer != null) {
            customWriter = writer
            serviceLogger.writer.close()
            serviceLogger.writer = writer
        } else {
            if (customWriter != null) {
                serviceLogger.writer.close()
                customWriter = null
                serviceLogger.writer = createLogger(configStr).writer
            }

        }
    }

    private suspend fun createAndRunServiceInstance(args: Array<String>, configuration: String) {

        val logs = serviceLogger.getCtxLoggers(className, "createAndRunServiceInstance")
        // create instance of the actual service to run
        try {
            serviceInstance = createServiceInstance(args, configuration, serviceLogger)
            logs.info("Created instance of service ${serviceInstance!!::class.java.simpleName}")
        } catch (e: Exception) {
            logs.errorEx("Error creating service instance: ${e.message}", e)
        }
        // run the service
        try {
            logs.info("Running service instance")
            runService()
        } catch (e: Exception) {
            logs.errorEx("Error starting service instance", e)

        }
    }

    private suspend fun runService() = coroutineScope {

        try {
            if (serviceInstance == null) {
                serviceLogger.getCtxErrorLog(className, "runService")("Could not create service instance")
                exitProcess(1)
            }
            serviceInstance?.start()

            serviceInstance?.blockUntilShutdown()
        }catch (e : Exception){
            serviceLogger.getCtxErrorLogEx(className, "runService")("Error running service", e)
        }

    }

    /**
     * Stops a running service instance
     */
    private fun stopService() {
        runBlocking {
            if (serviceInstance != null) {
                val log = serviceLogger.getCtxInfoLog(className, "StopServiceInstance")
                log("Stopping service instance")
                serviceInstance?.stop()
            }
        }
    }

    // Inherited classes that run service need to override this method to create an instance of the service
    // from the passed arguments and configuration reader
    abstract fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service


}