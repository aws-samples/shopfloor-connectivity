// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.log

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BYTES_SUFFIX
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PRIVATE_KEY
import com.amazonaws.sfc.config.ClientConfiguration.Companion.CONFIG_ROOT_CA
import com.amazonaws.sfc.config.ClientProxyConfiguration
import com.amazonaws.sfc.config.ConfigReader.Companion.createConfigReader
import com.amazonaws.sfc.config.ConfigReader.Companion.parsePlaceHolders
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WARNINGS
import com.amazonaws.sfc.metrics.MetricsCollectorMethod
import com.amazonaws.sfc.metrics.MetricsValue
import com.amazonaws.sfc.metrics.MetricsValueParam
import com.amazonaws.sfc.util.toStringEx
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord


typealias LogFunction = (String, String?, Exception?) -> Unit

interface LogWriter {
    fun write(logLevel: LogLevel, timestamp: Long, source: String?, message: String)
    fun close()
}


class Logger(
    var level: LogLevel = LogLevel.INFO,
    val source: String? = null,
    private val sourceFilter: Array<String>? = null,
    secretNames: Set<String>? = null,
    secretValues: Set<String>? = null,
    private val hiddenText: String = HIDDEN_VALUE,
    var writer: LogWriter
) {


    private var _secretNames = if (!secretNames.isNullOrEmpty()) secretNames as MutableSet<String> else mutableSetOf()
    private var _secretValues = if (!secretValues.isNullOrEmpty()) secretValues as MutableSet<String> else mutableSetOf()

    private var _metricsCollectorMethod: MetricsCollectorMethod? = null
    var metricsCollectorMethod
        get() = _metricsCollectorMethod
        set(value) {
            // write once as the "owner" og the logger might have set the handler.
            // For a connector/target running as an IPC service the connector/target will set the method to use the
            // metrics collector used for the connector/target service to create metrics for logged errors/warnings.
            // If the connector/target is running in process the collector of the sfc core will be used, so the property
            // is already set and should not be set to the method using the collector of that connector/target.
            if (_metricsCollectorMethod == null) {
                _metricsCollectorMethod = value
            }
        }

    init {
        _secretNames.addAll(listOf(ClientProxyConfiguration.CONFIG_PROXY_USERNAME, ClientProxyConfiguration.CONFIG_PROXY_PASSWORD))
    }

    fun addSecretsFieldsFromConfig(configString: String) {
        addSecretFields(getNamesWithSecretValues(configString))
    }

    fun addSecrets(secrets: Map<String, String>?) {
        if (secrets.isNullOrEmpty()) return
        addSecretFields(secrets.keys)
        addSecretsValues(secrets.values.toSet())
    }

    fun addSecretFields(fieldNames: Set<String>) {
        _secretNames.addAll(fieldNames)
    }

    fun addSecretsValues(secretValues: Set<String>) {
        _secretValues.addAll(secretValues)
    }

    private fun hideConfigSecrets(configStr: String): String {

        var str = configStr

        HIDDEN_FIELDS.forEach {
            val certBytesRegex = ("\"$it$CONFIG_BYTES_SUFFIX\"\\s*:\\s*\\[.*?]").toRegex()
            str = certBytesRegex.replace(str, "\"$it$CONFIG_BYTES_SUFFIX\":[$hiddenText]")
        }


        _secretNames.forEach {
            val secretNameRegex = (""""$it"\s*:\s*".*?"""").toRegex()
            str = secretNameRegex.replace(str, "\"$it\":$hiddenText")
        }

        _secretValues.forEach {
            str = str.replace(it, HIDDEN_VALUE)
        }

        return str
    }


    // filters on log level and source filter, calls emitter if message needs to be logged
    private fun emit(level: LogLevel, source: String?, message: String) {
        if (level <= this.level) {
            if ((source == null) || sourceFilter.isNullOrEmpty() || sourceFilter.contains(source)) {
                val msg = hideConfigSecrets(message)
                writer.write(level, System.currentTimeMillis(), source, cleanArrayString(msg))
            }
        }
    }

    // method for emitting trace messages
    private fun logTrace(message: String, source: String? = this.source, exception: Exception? = null) {
        if (level == LogLevel.TRACE) {
            if (exception != null) {
                emit(LogLevel.TRACE, source, message + " : ${exception.toStringEx()}")
            } else {
                emit(LogLevel.TRACE, source, message)
            }
        }
    }


    // method for emitting warning messages
    private fun logWarning(message: String, source: String? = this.source, exception: Exception? = null) {
        if (level >= LogLevel.WARNING) {
            if (level == LogLevel.TRACE && exception != null) {
                emit(LogLevel.WARNING, source, "$message  : ${exception.toStringEx()}")
            } else {
                var msg = message
                if (exception != null) msg += ", $exception"
                emit(LogLevel.WARNING, source, msg)
            }
        }
        try {
            metricsCollectorMethod?.let { it(listOf(MetricsValueParam(METRICS_WARNINGS, MetricsValue(1), MetricUnits.COUNT))) }
        } catch (_: Exception) {
        }
    }

    // method for emitting informational messages
    private fun logInfo(message: String, source: String? = this.source, exception: Exception? = null) {
        if (level >= LogLevel.INFO) {
            if (level == LogLevel.TRACE && exception != null) {
                emit(LogLevel.INFO, source, message + " : ${exception.toStringEx()}")
            } else {
                var msg = message
                if (exception != null) msg += ", $exception"
                emit(LogLevel.INFO, source, msg)
            }
        }
    }

    // method for emitting error messages
    private fun logError(message: String, source: String? = this.source, exception: Exception? = null) {
        if (exception != null) {
            emit(level, source, message + " : ${exception.toStringEx()}")
        } else {
            emit(LogLevel.ERROR, source, message)
        }
        try {
            metricsCollectorMethod?.let { it(listOf(MetricsValueParam(METRICS_ERRORS, MetricsValue(1), MetricUnits.COUNT))) }
        } catch (_: Exception) {
        }
    }

    /**
     * Method for logging errors
     */
    val error: LogFunction
        get() = this::logError

    /**
     * Gets the method to use for logging errors using a class instance and a method variable to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(ctxInstance: Any, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxErrorLogEx(logSource(LogLevel.ERROR, ctxInstance, ctxMethodVar))

    fun getCtxErrorLog(ctxInstance: Any, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxErrorLog(logSource(LogLevel.ERROR, ctxInstance, ctxMethodVar))

    /**
     * Gets the method to use for logging errors using a class instance and a method name to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(ctxInstance: Any, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxErrorLogEx(logSource(LogLevel.ERROR, ctxInstance, ctxMethod))

    fun getCtxErrorLog(ctxInstance: Any, ctxMethod: String): ((String) -> Unit) =
        getCtxErrorLog(logSource(LogLevel.ERROR, ctxInstance, ctxMethod))

    /**
     * Gets the method to use for logging errors using a class name and a method variable to build the source of the messages
     * @param ctxName String
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(ctxName: String, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxErrorLogEx(logSource(LogLevel.ERROR, ctxName, ctxMethodVar))

    fun getCtxErrorLog(ctxName: String, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxErrorLog(logSource(LogLevel.ERROR, ctxName, ctxMethodVar))

    /**
     * Get a method to use for logging errors using a class name and a method name to build the source of the messages
     * @param ctxName String
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(ctxName: String, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxErrorLogEx(logSource(ctxName, ctxMethod))

    fun getCtxErrorLog(ctxName: String, ctxMethod: String): ((String) -> Unit) =
        getCtxErrorLog(logSource(ctxName, ctxMethod))


    /**
     * Get a method to use for logging errors using a method variable to build the source of the messages
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxErrorLogEx(logSource(LogLevel.ERROR, ctxMethodVar))

    fun getCtxErrorLog(ctxMethodVar: Any): ((String) -> Unit) =
        getCtxErrorLog(logSource(LogLevel.ERROR, ctxMethodVar))

    /**
     * Gets the method for logging errors
     * @param logSource String
     * @return (String) -> Unit
     */
    fun getCtxErrorLogEx(logSource: String): ((String, Exception?) -> Unit) {
        return { s: String, exception: Exception? ->
            error(s, logSource, exception)
        }
    }

    fun getCtxErrorLog(logSource: String): ((String) -> Unit) {
        return { s: String ->
            error(s, logSource, null)
        }
    }

    /**
     * Method to use to log trace messages, evaluates to an empty function if loglevel is below Trace level
     */
    val trace: LogFunction
        get() = if (this.level == LogLevel.TRACE) this::logTrace else { _: String, _: String?, _: Exception? ->
        }

    /**
     * Get a method to use for logging trace messages using a class instance and a method variable to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(ctxInstance: Any, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxTraceLogEx(logSource(LogLevel.TRACE, ctxInstance, ctxMethodVar))

    fun getCtxTraceLog(ctxInstance: Any, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxTraceLog(logSource(LogLevel.TRACE, ctxInstance, ctxMethodVar))

    /**
     * Get a method to use for logging trace messages using a class instance and a method name to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(ctxInstance: Any, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxTraceLogEx(logSource(LogLevel.TRACE, ctxInstance, ctxMethod))

    fun getCtxTraceLog(ctxInstance: Any, ctxMethod: String): ((String) -> Unit) =
        getCtxTraceLog(logSource(LogLevel.TRACE, ctxInstance, ctxMethod))

    /**
     * Get a method to use for logging trace messages using a class name and a method variable to build the source of the messages
     * @param ctxName String
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(ctxName: String, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxTraceLogEx(logSource(LogLevel.TRACE, ctxName, ctxMethodVar))

    fun getCtxTraceLog(ctxName: String, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxTraceLog(logSource(LogLevel.TRACE, ctxName, ctxMethodVar))


    /**
     * Get a method to use for logging trace messages using a class name and a method name to build the source of the messages
     * @param ctxName String
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(ctxName: String, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxTraceLogEx(logSource(ctxName, ctxMethod))

    fun getCtxTraceLog(ctxName: String, ctxMethod: String): ((String) -> Unit) =
        getCtxTraceLog(logSource(ctxName, ctxMethod))

    /**
     * Get a method to use for logging trace messages using a method variable to build the source of the messages
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxTraceLogEx(logSource(LogLevel.TRACE, ctxMethodVar))

    fun getCtxTraceLog(ctxMethodVar: Any): ((String) -> Unit) =
        getCtxTraceLog(logSource(LogLevel.TRACE, ctxMethodVar))

    /**
     * Get a method for logging trace messages
     * @param logSource String
     * @return (String) -> Unit
     */
    fun getCtxTraceLogEx(logSource: String): ((String, Exception?) -> Unit) {
        return { s: String, exception: Exception? ->
            trace(s, logSource, exception)
        }
    }

    fun getCtxTraceLog(logSource: String): ((String) -> Unit) {
        return { s: String ->
            trace(s, logSource, null)
        }
    }

    /**
     * Method to use to log warning messages, evaluates to an empty function if loglevel is below WARNING level
     */
    val warning: LogFunction
        get() = if (this.level >= LogLevel.WARNING) this::logWarning else { _: String, _: String?, _: Exception? ->
        }

    /**
     * Get a method to use for logging warning messages using a class instance and a method variable to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(ctxInstance: Any, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxWarningLogEx(logSource(LogLevel.WARNING, ctxInstance, ctxMethodVar))

    fun getCtxWarningLog(ctxInstance: Any, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxWarningLog(logSource(LogLevel.WARNING, ctxInstance, ctxMethodVar))


    /**
     * Get a method to use for logging trace messages using a class instance and a method name to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(ctxInstance: Any, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxWarningLogEx(logSource(LogLevel.WARNING, ctxInstance, ctxMethod))

    fun getCtxWarningLog(ctxInstance: Any, ctxMethod: String): ((String) -> Unit) =
        getCtxWarningLog(logSource(LogLevel.WARNING, ctxInstance, ctxMethod))

    /**
     * Get a method to use for logging warning messages using a class name and a method variable to build the source of the messages
     * @param ctxName String
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(ctxName: String, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxWarningLogEx(logSource(LogLevel.WARNING, ctxName, ctxMethodVar))

    fun getCtxWarningLog(ctxName: String, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxWarningLog(logSource(LogLevel.WARNING, ctxName, ctxMethodVar))

    /**
     * Get a method to use for logging warning messages using a class name and a method name to build the source of the messages
     * @param ctxName String
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(ctxName: String, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxWarningLogEx(logSource(ctxName, ctxMethod))

    fun getCtxWarningLog(ctxName: String, ctxMethod: String): ((String) -> Unit) =
        getCtxWarningLog(logSource(ctxName, ctxMethod))

    /**
     * Get a method to use for logging trace messages using a method variable to build the source of the messages
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxWarningLogEx(logSource(LogLevel.WARNING, ctxMethodVar))

    fun getCtxWarningLog(ctxMethodVar: Any): ((String) -> Unit) =
        getCtxWarningLog(logSource(LogLevel.WARNING, ctxMethodVar))

    /**
     * Get a method for logging warning messages
     * @param logSource String
     * @return (String) -> Unit
     */
    fun getCtxWarningLogEx(logSource: String): ((String, Exception?) -> Unit) {
        return { s: String, exception: Exception? ->
            warning(s, logSource, exception)
        }
    }

    fun getCtxWarningLog(logSource: String): ((String) -> Unit) {
        return { s: String ->
            warning(s, logSource, null)
        }
    }

    /**
     * Method to use to log informational messages, evaluates to an empty function if loglevel is below INFO level
     */
    val info: LogFunction
        get() = if (this.level >= LogLevel.INFO) this::logInfo else { _: String, _: String?, _: Exception? ->
        }

    /**
     * Get a method to use for logging informational messages using a class instance and a method variable to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxInfoLogEx(ctxInstance: Any, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxInfoLogEx(logSource(LogLevel.INFO, ctxInstance, ctxMethodVar))

    fun getCtxInfoLog(ctxInstance: Any, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxInfoLog(logSource(LogLevel.INFO, ctxInstance, ctxMethodVar))

    /**
     * Get a method to use for logging informational messages using a class instance and a method name to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxInfoLogEx(ctxInstance: Any, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxInfoLogEx(logSource(LogLevel.INFO, ctxInstance, ctxMethod))

    fun getCtxInfoLog(ctxInstance: Any, ctxMethod: String): ((String) -> Unit) =
        getCtxInfoLog(logSource(LogLevel.INFO, ctxInstance, ctxMethod))

    /**
     * Get a method to use for logging informational messages using a class name and a method variable to build the source of the messages
     * @param ctxName String
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxInfoLogEx(ctxName: String, ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxInfoLogEx(logSource(LogLevel.INFO, ctxName, ctxMethodVar))

    fun getCtxInfoLog(ctxName: String, ctxMethodVar: Any): ((String) -> Unit) =
        getCtxInfoLog(logSource(LogLevel.INFO, ctxName, ctxMethodVar))

    /**
     * Get a method to use for logging informational messages using a class name and a method name to build the source of the messages
     * @param ctxName String
     * @param ctxMethod String
     * @return (String) -> Unit
     */
    fun getCtxInfoLogEx(ctxName: String, ctxMethod: String): ((String, Exception?) -> Unit) =
        getCtxInfoLogEx(logSource(ctxName, ctxMethod))

    fun getCtxInfoLog(ctxName: String, ctxMethod: String): ((String) -> Unit) =
        getCtxInfoLog(logSource(ctxName, ctxMethod))

    /**
     * Get a method to use for logging informational messages using a method variable to build the source of the messages
     * @param ctxMethodVar Any
     * @return (String) -> Unit
     */
    fun getCtxInfoLogEx(ctxMethodVar: Any): ((String, Exception?) -> Unit) =
        getCtxInfoLogEx(logSource(LogLevel.INFO, ctxMethodVar))

    fun getCtxInfoLog(ctxMethodVar: Any): ((String) -> Unit) =
        getCtxInfoLog(logSource(LogLevel.INFO, ctxMethodVar))

    /**
     * Get a method for logging informational messages
     * @param logSource String
     * @return (String) -> Unit
     */

    fun getCtxInfoLogEx(logSource: String): ((String, Exception?) -> Unit) {
        return { s: String, exception: Exception? ->
            info(s, logSource, exception)
        }
    }

    fun getCtxInfoLog(logSource: String): ((String) -> Unit) {
        return { s: String ->
            info(s, logSource, null)
        }
    }

    /**
     * Gets a context logger using a class name and a method variable to build the source of the messages
     * @param ctxName String
     * @param ctxMethodInst Any
     * @return ContextLogger
     */
    fun getCtxLoggers(ctxName: String, ctxMethodInst: Any) =
        ContextLogger.getLoggers(this, logSource(ctxName, methodName(ctxMethodInst)))

    /**
     * Gets a context logger using a class instance and a method name to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethod String
     * @return ContextLogger
     */
    fun getCtxLoggers(ctxInstance: Any, ctxMethod: String) =
        ContextLogger.getLoggers(this, logSource(ctxInstance.javaClass.simpleName, ctxMethod))

    /**
     * Gets a context logger using a class instance and a method variable to build the source of the messages
     * @param ctxInstance Any
     * @param ctxMethodVar Any
     * @return ContextLogger
     */
    fun getCtxLoggers(ctxInstance: Any, ctxMethodVar: Any) =
        ContextLogger.getLoggers(this, logSource(ctxInstance.javaClass.simpleName, methodName(ctxMethodVar)))

    /**
     * Gets a context logger using a class name and a method name to build the source of the messages
     * @param ctxName String
     * @param ctxMethod String
     * @return ContextLogger
     */
    fun getCtxLoggers(ctxName: String, ctxMethod: String) =
        ContextLogger.getLoggers(this, logSource(ctxName, ctxMethod))

    /**
     * Gets a context logger using a method variable to build the source of the messages
     * @param ctxMethodVar Any
     * @return ContextLogger
     */
    fun getCtxLoggers(ctxMethodVar: Any) =
        ContextLogger.getLoggers(this, logSource(className(ctxMethodVar), methodName(ctxMethodVar)))

    /**
     * Gets a context logger using the name of the source of the messages
     * @param logSource String
     * @return ContextLogger
     */
    fun getCtxLoggers(logSource: String): ContextLogger = ContextLogger.getLoggers(this, logSource)

    // builds the log source from a class name and an instance of a method variable
    private fun logSource(logLevel: LogLevel, ctxName: String, ctxMethodInst: Any) =
        if (logLevel <= level) logSource(ctxName, methodName(ctxMethodInst)) else ""

    // builds the log source from a class instance and a method name
    private fun logSource(logLevel: LogLevel, ctxInstance: Any, ctxMethod: String) =
        if (logLevel <= level) logSource(ctxInstance.javaClass.simpleName, ctxMethod) else ""

    // builds the log source from a class instance and an instance of a method variable
    private fun logSource(logLevel: LogLevel, ctxInstance: Any, ctxMethodVar: Any) =
        if (logLevel <= level) logSource(ctxInstance.javaClass.simpleName, methodName(ctxMethodVar)) else ""

    // builds the log source from an instance of a method variable
    private fun logSource(logLevel: LogLevel, ctxMethodInst: Any) =
        if (logLevel <= level) logSource(className(ctxMethodInst), methodName(ctxMethodInst)) else ""

    // builds the log source from a class name and a method name
    private fun logSource(ctxName: String, ctxMethod: String) = "$ctxName:$ctxMethod"

    companion object {

        fun createLogger() = Logger(LogLevel.INFO, source = null, sourceFilter = null, secretNames = null, writer = ConsoleLogWriter())

        fun createLogger(configString: String): Logger {
            val secrets = getNamesWithSecretValues(configString)
            return Logger(LogLevel.INFO, source = null, sourceFilter = null, secretNames = secrets, writer = ConsoleLogWriter())
        }

        fun redirectLoggers(logger: Logger, className: String) {

            LogManager.getLogManager().loggerNames.iterator().forEach { logName ->
                val log = LogManager.getLogManager().getLogger(logName)
                log?.handlers?.iterator()?.forEach { handler ->
                    log.removeHandler(handler)
                }
                log?.addHandler(object : Handler() {

                    val ctxLog = logger.getCtxLoggers(className, logName)
                    override fun publish(record: LogRecord) {
                        val msg = "${record.message}, ${record.thrown?.message}"
                        when (record.level) {
                            Level.INFO -> ctxLog.info(msg)
                            Level.FINE -> ctxLog.trace(msg)
                            Level.FINER -> ctxLog.trace(msg)
                            Level.FINEST -> ctxLog.trace(msg)
                            Level.WARNING -> ctxLog.warning(msg)
                            Level.SEVERE -> ctxLog.error(msg)
                            Level.CONFIG -> ctxLog.trace(msg)
                        }
                    }

                    override fun flush() {}
                    override fun close() {}
                })

            }
        }

        const val CONF_LOG_WRITER = "LogWriter"

        private val continuation = "${kotlin.coroutines.Continuation::class.java.name})"


        // gets the name of the method of a variable in the scope of that method
        internal fun methodName(ctxMethodVar: Any): String {
            return when (val s = ctxMethodVar.javaClass.enclosingMethod.name) {
                "invokeSuspend", "invoke" -> ctxMethodVar.javaClass.enclosingMethod.toString().split("$").last()
                else -> s
            }
        }

        // gets the name of class for the method for a variable in the scope of a method of that class
        internal fun className(ctxMethodVar: Any): String {

            val s = ctxMethodVar.javaClass.enclosingMethod.toString()
            return when {
                s.endsWith(continuation) -> {
                    val tmp = s.split("(")[0].split(".")
                    tmp[tmp.size - minOf(tmp.size, 2)]
                }

                s.endsWith("()") && s.split(".").last().startsWith("get") -> {
                    val tmp = s.split(".")
                    tmp[tmp.size - minOf(tmp.size, 2)]
                }

                else -> {
                    var tmp = s.split("(")[0].split("$")
                    val i = tmp.size
                    tmp = tmp[0].split(".")
                    tmp[tmp.size - (if (i == 1) 2 else 1)]
                }
            }

        }

        private fun getNamesWithSecretValues(configString: String): Set<String> {
            val config =
                createConfigReader(configStr = configString, allowUnresolved = true, secretsManager = null).getConfig<BaseConfiguration>(validate = false)
            val secretNames = config.secretPlaceholderNames ?: emptyList()

            val keys = parsePlaceHolders(configString)
            return keys.filter { secretNames.contains(it.second) }.map { it.first }.toSet()
        }

        fun cleanArrayString(input: String): String {
            if (!input.contains("[")) return input

            val output = arrayRegex.replace(input) { result ->
                val array = result.value
                val cleanedArray = array.replace(cleanupRegex) { match ->
                    if (match.groups[1] != null) { // quoted value
                        match.groups[1]!!.value
                    } else { // remove spaces
                        ""
                    }
                }
                cleanedArray
            }
            return output
        }

        private val arrayRegex = Regex("""\[[^\[\]]*]""")
        private val cleanupRegex = Regex("""("[^"]*")|\s+""")

        private val HIDDEN_FIELDS = listOf(CONFIG_CERTIFICATE, CONFIG_ROOT_CA, CONFIG_PRIVATE_KEY)
        const val HIDDEN_VALUE = "*** hidden ***"
    }


    /**
     * A context logger holds the methods to log error messages at error, warning, info  and trace level.
     * Depending on the trace level that was used when creating an instance of this class methods for
     * log levels above the specified level may be methods that do not emit the message to the actual logger.
     * @property error LogFunction Function for logging error messages
     * @property warning LogFunction Function for logging warning messages
     * @property info LogFunction Function for logging informational messages
     * @property trace LogFunction Function for logging trace messages
     * @see LogLevel
     */
    class ContextLogger(
        val error: ((String) -> Unit),
        val errorEx: ((String, Exception?) -> Unit),
        val warning: ((String) -> Unit),
        val warningEx: ((String, Exception?) -> Unit),
        val info: ((String) -> Unit),
        val infoEx: ((String, Exception?) -> Unit),
        val trace: ((String) -> Unit),
        val traceEx: ((String, Exception?) -> Unit)

    ) {
        companion object {
            /**
             * Gets a set of log methods for the specified level and source
             * @param logger Logger
             * @param logSource String
             * @return ContextLogger
             */
            fun getLoggers(logger: Logger, logSource: String): ContextLogger {

                return ContextLogger(
                    error = logger.getCtxErrorLog(logSource),
                    errorEx = logger.getCtxErrorLogEx(logSource),
                    warning = logger.getCtxWarningLog(logSource),
                    warningEx = logger.getCtxWarningLogEx(logSource),
                    info = logger.getCtxInfoLog((logSource)),
                    infoEx = logger.getCtxInfoLogEx((logSource)),
                    trace = logger.getCtxTraceLog(logSource),
                    traceEx = logger.getCtxTraceLogEx(logSource)
                )
            }
        }
    }
}






