
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filetarget.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.Compress.CONFIG_COMPRESS
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.filetarget.config.FileTargetWriterConfiguration.Companion.FILE_TARGET
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import java.nio.file.Files
import kotlin.io.path.Path


@ConfigurationClass
class FileTargetConfiguration : TargetConfiguration(), Validate {

    @SerializedName(CONFIG_DIRECTORY)
    private var _directory: String? = null

    val directory
        get() = Path(_directory ?: "")

    @SerializedName(CONFIG_UTC_TIME)
    private var _utcTime: Boolean = false

    val utcTime: Boolean
        get() = _utcTime

    @SerializedName(CONFIG_JSON)
    private var _json: Boolean = true

    val json: Boolean
        get() = _json

    @SerializedName(CONFIG_EXTENSION)
    private var _extension: String = ""

    val extension: String
        get() = if (_extension.startsWith(".") || _extension.isBlank()) _extension else ".$_extension"

    @SerializedName(CONFIG_BUFFER_SIZE)
    private var _bufferSize: Int = DEFAULT_BUFFER_SIZE // in KB

    /**
     * Batch size in bytes for writing data to file
     */
    val bufferSize: Int
        get() = _bufferSize * 1024  // to KB

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int = DEFAULT_INTERVAL // in seconds

    /**
     * Interval in milliseconds for writing data to file
     */
    val interval: Int
        get() = _interval * 1000 // to milliseconds


    @SerializedName(CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE


    override fun validate() {
        if (validated) return
        super.validate()
        validateDirectory()
        validateBufferingInterval()
        validateBufferingSize()
        validated = true
    }

    private fun validateDirectory() {
        ConfigurationException.check(
            _directory != null,
            "$CONFIG_DIRECTORY must be specified",
            CONFIG_DIRECTORY,
            this
        )

        ConfigurationException.check(
            Files.exists(directory),
            "$CONFIG_DIRECTORY ${directory.toAbsolutePath()} does not exist",
            CONFIG_DIRECTORY,
            this
        )
    }

    // validates buffering interval
    private fun validateBufferingInterval() =
        ConfigurationException.check(
            (_interval in 60..900),
            "$CONFIG_INTERVAL must be in range 60..900 seconds",
            CONFIG_INTERVAL,
            this
        )

    // validates buffering interval
    private fun validateBufferingSize() =
        ConfigurationException.check(
            (_bufferSize in 1..1024),
            "$CONFIG_BUFFER_SIZE must be in range 1..1024 KB",
            CONFIG_BUFFER_SIZE,
            this
        )

    companion object {
        private const val CONFIG_DIRECTORY = "Directory"
        private const val CONFIG_UTC_TIME = "UtcTime"
        private const val CONFIG_JSON = "Json"
        private const val CONFIG_EXTENSION = "Extension"
        private const val CONFIG_BUFFER_SIZE = "BufferSize"
        private const val DEFAULT_BUFFER_SIZE = 16
        private const val DEFAULT_INTERVAL = 60

        private val default = FileTargetConfiguration()

        @Suppress("unused")
        fun create(directory: String? = default._directory,
                   utcTime: Boolean = default._utcTime,
                   json: Boolean = default._json,
                   extension: String = default._extension,
                   bufferSize: Int = default._bufferSize,
                   interval: Int = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   compressionType: CompressionType? = default._compressionType,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): FileTargetConfiguration {

            val instance = createTargetConfiguration<FileTargetConfiguration>(description = description,
                active = active,
                targetType = FILE_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as FileTargetConfiguration

            with(instance) {
                _directory = directory
                _utcTime = utcTime
                _json = json
                _extension = extension
                _bufferSize = bufferSize
                _interval = interval
                _compressionType = compressionType
            }
            return instance
        }


    }

}