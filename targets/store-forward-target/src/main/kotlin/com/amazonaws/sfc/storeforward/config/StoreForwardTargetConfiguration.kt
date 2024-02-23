
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.storeforward.config.StoreForwardWriterConfiguration.Companion.STORE_FORWARD
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class StoreForwardTargetConfiguration : TargetConfiguration(), Validate, MessageBufferConfiguration {

    @SerializedName(CONFIG_WRITE_TIMEOUT)
    private var _writeTimeout: Int = CONFIG_WRITE_TIMEOUT_DEFAULT
    override val writeTimeout: Duration
        get() {
            return _writeTimeout.toDuration(DurationUnit.SECONDS)
        }

    @SerializedName(CONFIG_RETAIN_PERIOD)
    private var _retainPeriod: Int? = null
    override val retainPeriod: Duration?
        get() {
            return _retainPeriod?.toDuration(DurationUnit.MINUTES)
        }

    @SerializedName(CONFIG_RETAIN_FILES)
    private var _retainFiles: Int? = null
    override val retainNumber: Int?
        get() {
            return _retainFiles
        }


    @SerializedName(CONFIG_RETAIN_SIZE)
    private var _retainSize: Long? = null
    override val retainSize: Long?
        get() {
            return if (_retainSize != null) (_retainSize?.times(1000000L)) else null
        }

    @SerializedName(CONFIG_FIFO)
    private var _fifo: Boolean = CONFIG_FIFO_DEFAULT
    override val fifo: Boolean
        get() {
            return _fifo
        }


    @SerializedName(CONFIG_CLEANUP_INTERVAL)
    private var _cleanupInterval: Long = DEFAULT_CONFIG_CLEANUP_INTERVAL
    override val cleanupInterval: Duration
        get() {
            return _cleanupInterval.toDuration(DurationUnit.SECONDS)
        }

    @SerializedName(CONFIG_COMPRESSION)
    private var _compression = false
    val compression: Boolean
        get() = _compression


    @SerializedName(CONFIG_DIRECTORY)
    private var _directory: String? = null
    val directory: File?
        get() = if (_directory != null) File(_directory!!) else null

    override fun validate() {
        if (validated) return
        super.validate()

        validateDirectory()

        checkRetention()

        validated = true
    }

    private fun checkRetention() {
        ConfigurationException.check(
            listOfNotNull(retainSize, retainNumber, retainPeriod).size > 1,
            "Only one retain criteria can be specified",
            "$CONFIG_RETAIN_FILES, $CONFIG_RETAIN_PERIOD, $CONFIG_RETAIN_SIZE",
            this
        )

        ConfigurationException.check(
            listOfNotNull(retainSize, retainNumber, retainPeriod).isNotEmpty(),
            "At least one retain criteria can be specified in order to prevent running out of storage on the storage device",
            "$CONFIG_RETAIN_FILES, $CONFIG_RETAIN_PERIOD, $CONFIG_RETAIN_SIZE",
            this
        )

        ConfigurationException.check(
            (retainSize == null || retainSize!! > 0),
            "$CONFIG_RETAIN_SIZE must be at least 1",
            CONFIG_RETAIN_SIZE,
            this
        )

        ConfigurationException.check(
            (_retainFiles == null || _retainFiles!! >= 100),
            "$CONFIG_RETAIN_FILES must be at least 100",
            CONFIG_RETAIN_FILES,
            this
        )

        ConfigurationException.check(
            (_retainPeriod == null || retainPeriod!! >= 5.toDuration(DurationUnit.MINUTES)),
            "$CONFIG_RETAIN_PERIOD must be at least 5 (minutes)",
            CONFIG_RETAIN_PERIOD,
            this
        )
    }

    private fun validateDirectory() {
        ConfigurationException.check(
            !_directory.isNullOrEmpty(),
            "$CONFIG_DIRECTORY must be specified",
            CONFIG_DIRECTORY,
            this
        )

        ConfigurationException.check(
            (directory?.exists() == true && directory?.isDirectory == true),
            "$CONFIG_DIRECTORY directory does not exist",
            CONFIG_DIRECTORY,
            this
        )
    }

    companion object {
        const val CONFIG_DIRECTORY = "Directory"
        const val CONFIG_WRITE_TIMEOUT = "WriteTimeout"
        const val CONFIG_WRITE_TIMEOUT_DEFAULT = 10
        const val CONFIG_RETAIN_PERIOD = "RetainPeriod"
        const val CONFIG_RETAIN_FILES = "RetainFiles"
        const val CONFIG_RETAIN_SIZE = "RetainSize"
        const val CONFIG_FIFO = "Fifo"
        const val CONFIG_FIFO_DEFAULT = true
        const val CONFIG_CLEANUP_INTERVAL = "CleanupInterval"
        const val DEFAULT_CONFIG_CLEANUP_INTERVAL = 60L
        const val CONFIG_COMPRESSION = "Compression"

        private val default = StoreForwardTargetConfiguration()

        fun create(writeTimeout: Int = default._writeTimeout,
                   retainPeriod: Int? = default._retainPeriod,
                   retainFiles: Int? = default._retainFiles,
                   retainSize: Long? = default._retainSize,
                   fifo: Boolean = default._fifo,
                   compression: Boolean = default._compression,
                   cleanupInterval: Long = default._cleanupInterval,
                   directory: String? = default._directory,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   credentialProviderClient: String? = default._credentialProvideClient,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   targets: List<String>? = default._subTargets): StoreForwardTargetConfiguration {

            val instance = createTargetConfiguration<StoreForwardTargetConfiguration>(description = description,
                active = active,
                targetType = STORE_FORWARD,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as StoreForwardTargetConfiguration

            @Suppress("DuplicatedCode")
            with(instance) {
                _writeTimeout = writeTimeout
                _retainPeriod = retainPeriod
                _retainFiles = retainFiles
                _retainSize = retainSize
                _compression = compression
                _fifo = fifo
                _cleanupInterval = cleanupInterval
                _directory = directory
                _subTargets = targets
            }
            return instance
        }

    }
}