// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.zenohtarget.config

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
class ZenohTargetConfiguration : TargetConfiguration(), Validate {


    @SerializedName(CONFIG_KEYEXPR)
    private var _keyexpr: String? = null
    val keyexpr: String
        get() = _keyexpr ?: ""

    @SerializedName(CONFIG_ALTERNATE_KEYEXPR)
    private var _alternateKeyexpr: String? = null
    val alternateKeyexpr: String?
        get() = _alternateKeyexpr

    @SerializedName(CONFIG_WARN_ALTERNATE_KEYEXPR)
    private var _warnAlternateKeyexpr: Boolean = true
    val warnAlternateKeyexpr: Boolean
        get() = _warnAlternateKeyexpr

    @SerializedName(CONFIG_ZENOH_CONNECT_ENDPOINTS)
    private var _zenohConnectEndpoints : String? = null
    val zenohConnectEndpoints
        get() = _zenohConnectEndpoints

    // can only be "peer" or "client" or null
    @SerializedName(CONFIG_ZENOH_MODE)
    private var _zenohMode : String? = null
    val zenohMode
        get() = _zenohMode

    @SerializedName(CONFIG_ZENOH_LISTEN_ENDPOINTS)
    private var _zenohListenEndpoints : String? = null
    val zenohListenEndpoints
        get() = _zenohListenEndpoints

    @SerializedName(CONFIG_ZENOH_DISABLE_MULTICAST_SCOUTING)
    private var _zenohDisableMulticastScouting : Boolean = false
    val zenohDisableMulticastScouting
        get() = _zenohDisableMulticastScouting

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
            _zenohConnectEndpoints != null,
            "$CONFIG_ZENOH_CONNECT_ENDPOINTS must be set",
            CONFIG_ZENOH_CONNECT_ENDPOINTS,
            this
        )
        ConfigurationException.check(
            _zenohMode == null || _zenohMode.equals("client") || _zenohMode.equals("peer"),
            "$CONFIG_ZENOH_MODE must be not set or have the values 'peer' or 'client'",
            CONFIG_ZENOH_MODE,
            this
        )
    }

    private fun validateBufferSize() {
        ConfigurationException.check(
            (_batchSize == null || _maxPayloadSize == null || _compressionType == CompressionType.NONE || (_batchSize!! <= _maxPayloadSize!!)),
            "$CONFIG_BATCH_SIZE must be smaller or equal to value of $CONFIG_BATCH_SIZE",
            CONFIG_BATCH_SIZE,
            this)
    }

    companion object {

        const val CONFIG_ZENOH_CONNECT_ENDPOINTS = "ZenohConnectEndpoints"
        const val CONFIG_ZENOH_MODE = "ZenohMode"
        const val CONFIG_ZENOH_LISTEN_ENDPOINTS = "ZenohListenEndpoints"
        const val CONFIG_ZENOH_DISABLE_MULTICAST_SCOUTING = "DisableMulticastScouting"
        const val CONFIG_KEYEXPR = "Keyexpr"
        const val CONFIG_ALTERNATE_KEYEXPR = "AlternateKeyexpr"
        const val CONFIG_WARN_ALTERNATE_KEYEXPR = "WarnAlternateKeyexpr"
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_BATCH_COUNT = "BatchCount"
        const val CONFIG_BATCH_INTERVAL = "BatchInterval"
        private const val CONFIG_MAX_PAYLOAD_SIZE = "MaxPayloadSize"
        val default = ZenohTargetConfiguration()


        fun create(
            keyexpr: String? = default._keyexpr,
            alternateKeyexpr: String? = default._alternateKeyexpr,
            warnAlternateKeyexpr: Boolean = default._warnAlternateKeyexpr,
            zenohMode: String? = default._zenohMode,
            zenohConnectEndpoints: String? = default._zenohConnectEndpoints,
            zenohListenEndpoints: String? = default._zenohListenEndpoints,
            zenohDisableMulticastScouting: Boolean = default._zenohDisableMulticastScouting,
            batchCount: Int? = default.batchCount,
            batchSize: Int? = default._batchSize,
            batchInterval: Int? = default._batchInterval,
            maxPayloadSize: Int? = default._maxPayloadSize,
            compression: CompressionType? = default._compressionType
        ): ZenohTargetConfiguration {


            val instance = ZenohTargetConfiguration()
            with(instance) {
                _keyexpr = keyexpr
                _alternateKeyexpr = alternateKeyexpr
                _warnAlternateKeyexpr = warnAlternateKeyexpr
                _zenohMode = zenohMode
                _zenohConnectEndpoints = zenohConnectEndpoints
                _zenohListenEndpoints = zenohListenEndpoints
                _zenohDisableMulticastScouting = zenohDisableMulticastScouting
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
