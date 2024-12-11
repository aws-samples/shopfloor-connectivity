// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.natstarget.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.Compress.CONFIG_COMPRESS
import com.amazonaws.sfc.data.CompressionType
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ConfigurationClass
class NatsTargetConfiguration : TargetConfiguration(), Validate {


    @SerializedName(CONFIG_SUBJECT_NAME)
    private var _subjectName: String? = null
    val subjectName: String
        get() = _subjectName ?: ""

    @SerializedName(CONFIG_ALTERNATE_SUBJECT_NAME)
    private var _alternateSubjectName: String? = null
    val alternateSubjectName: String?
        get() = _alternateSubjectName

    @SerializedName(CONFIG_WARN_ALTERNATE_SUBJECT_NAME)
    private var _warnAlternateSubjectName: Boolean = true
    val warnAlternateSubjectName: Boolean
        get() = _warnAlternateSubjectName

    @SerializedName(CONFIG_NATS_SERVER)
    private var _natsServerConfiguration : NatsServerConfiguration? = null
    val natServerConfiguration
        get() = _natsServerConfiguration ?: NatsServerConfiguration()

    @SerializedName(CONFIG_PUBLISH_TIMEOUT)
    private var _publishTimeout = DEFAULT_PUBLISH_TIMEOUT

    val publishTimeout: Duration = _publishTimeout.toDuration(DurationUnit.SECONDS)
    @SerializedName(CONFIG_BATCH_COUNT)
    private var _batchCount: Int? = null
    val batchCount
        get() = _batchCount ?: 0

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int? = null
    val batchSize
        get() = if (_batchSize != null) _batchSize!! * 1024 else 0

    @SerializedName(CONFIG_BATCH_INTERVAL)
    private var _batchInterval: Int? = null
    val batchInterval: Duration
        get() = _batchInterval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE

    @SerializedName(CONFIG_MAX_PAYLOAD_SIZE)
    private var _maxPayloadSize: Int? = null
    val maxPayloadSize
        get() = if (_maxPayloadSize != null) _maxPayloadSize!! * 1024 else null


    @SerializedName(CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()

        validateBufferSize()
        validateServer()

        validated = true
    }

    private fun validateServer(){
        ConfigurationException.check(
            _natsServerConfiguration != null,
            "$CONFIG_NATS_SERVER must be set",
            CONFIG_NATS_SERVER,
            this
        )

        _natsServerConfiguration!!.validate()
    }

    private fun validateBufferSize() {
        ConfigurationException.check(
            (_batchSize == null || _maxPayloadSize == null || _compressionType == CompressionType.NONE || (_batchSize!! <= _maxPayloadSize!!)),
            "$CONFIG_BATCH_SIZE must be smaller or equal to value of $CONFIG_BATCH_SIZE",
            CONFIG_BATCH_SIZE,
            this)
    }


    companion object {

        const val CONFIG_NATS_SERVER = "NatsServer"
        const val CONFIG_SUBJECT_NAME = "SubjectName"
        const val CONFIG_ALTERNATE_SUBJECT_NAME = "AlternateSubjectName"
        const val CONFIG_WARN_ALTERNATE_SUBJECT_NAME = "WarnAlternateSubjectName"
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_BATCH_COUNT = "BatchCount"
        const val CONFIG_BATCH_INTERVAL = "BatchInterval"
        private const val CONFIG_MAX_PAYLOAD_SIZE = "MaxPayloadSize"
        val default = NatsTargetConfiguration()


        fun create(
            subjectName: String? = default._subjectName,
            alternateSubjectName : String? = default.alternateSubjectName,
            warnAlternateSubjectName: Boolean = default._warnAlternateSubjectName,
            natsServerConfiguration: NatsServerConfiguration = default.natServerConfiguration,
            batchCount: Int? = default.batchCount,
            batchSize: Int? = default._batchSize,
            batchInterval: Int? = default._batchInterval,
            maxPayloadSize: Int? = default._maxPayloadSize,
            compression: CompressionType? = default._compressionType
        ): NatsTargetConfiguration {


            val instance = NatsTargetConfiguration()
            with(instance) {
                _subjectName = subjectName
                _alternateSubjectName = alternateSubjectName
                _warnAlternateSubjectName = warnAlternateSubjectName
                _natsServerConfiguration = natsServerConfiguration
                _batchCount = batchCount
                _batchSize = batchSize
                _batchInterval = batchInterval
                _maxPayloadSize = maxPayloadSize
                _compressionType = compression
            }
            return instance
        }


    }
}
