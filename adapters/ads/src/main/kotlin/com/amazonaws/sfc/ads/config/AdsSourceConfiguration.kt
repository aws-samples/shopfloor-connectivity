// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ads.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern

@ConfigurationClass
class AdsSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_DEVICE)
    private var _adapterDeviceID: String = ""
    val adapterDeviceID: String
        get() = _adapterDeviceID

    @SerializedName(CONFIG_SOURCE_AMS_NET_ID)
    private var _sourceAmsNet: String = ""
    val sourceAmsNet: String
        get() = _sourceAmsNet

    @SerializedName(CONFIG_SOURCE_AMS_PORT)
    private var _sourceAmsPort: Int = -1
    val sourceAmsPort: Int
        get() = _sourceAmsPort

    @SerializedName(CONFIG_TARGET_AMS_NET_ID)
    private var _targetAmsNet: String = ""
    val targetAmsNet: String
        get() = _targetAmsNet

    @SerializedName(CONFIG_TARGET_AMS_PORT)
    private var _targetAmsPort: Int = -1
    val targetAmsPort: Int
        get() = _targetAmsPort


    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, AdsChannelConfiguration>()
    val channels: Map<String, AdsChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveDevice()

        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validateAmsSource()
        validateAmsTarget()

        validated = true
    }


    private fun validateMustHaveDevice() =
        ConfigurationException.check(
            (_adapterDeviceID.isNotEmpty()),
            "$CONFIG_ADAPTER_DEVICE for ADS source must be set to a valid device for the used ADS adapter",
            CONFIG_ADAPTER_DEVICE,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "ADS source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    private fun validateAmsSource() {
        ConfigurationException.check(
            (_sourceAmsNet.isNotEmpty()),
            "$CONFIG_SOURCE_AMS_NET_ID for ADS source must be set",
            CONFIG_SOURCE_AMS_NET_ID,
            this
        )

        ConfigurationException.check(
            NET_ID_PATTERN.matcher(_sourceAmsNet).matches(),
            "$CONFIG_SOURCE_AMS_NET_ID format is wrong, it must contain 6 numeric elements",
            CONFIG_SOURCE_AMS_NET_ID,
            this
        )


        ConfigurationException.check(
            (_sourceAmsPort != -1),
            "$CONFIG_SOURCE_AMS_PORT for ADS source must be set",
            CONFIG_SOURCE_AMS_PORT,
            this
        )
    }

    private fun validateAmsTarget() {

        ConfigurationException.check(
            (_targetAmsNet.isNotEmpty()),
            "$CONFIG_TARGET_AMS_NET_ID for ADS target must be set",
            CONFIG_TARGET_AMS_NET_ID,
            this
        )

        ConfigurationException.check(
            NET_ID_PATTERN.matcher(_targetAmsNet).matches(),
            "$CONFIG_TARGET_AMS_NET_ID format is wrong, it must contain 6 elements",
            CONFIG_TARGET_AMS_NET_ID,
            this
        )

        ConfigurationException.check(
            (_targetAmsPort != -1),
            "$CONFIG_TARGET_AMS_PORT for ADS target must be set",
            CONFIG_TARGET_AMS_PORT,
            this
        )
    }

    companion object {
        const val CONFIG_ADAPTER_DEVICE = "AdapterDevice"
        const val CONFIG_SOURCE_AMS_NET_ID = "SourceAmsNetId"
        const val CONFIG_SOURCE_AMS_PORT = "SourceAmsPort"
        const val CONFIG_TARGET_AMS_NET_ID = "TargetAmsNetId"
        const val CONFIG_TARGET_AMS_PORT = "TargetAmsPort"


        val NET_ID_PATTERN: Pattern = Pattern.compile("\\d+(\\.\\d+){5}")

        private val default = AdsSourceConfiguration()

        fun create(
            channels: Map<String, AdsChannelConfiguration> = default._channels,
            targetAmsNetId: String = default._targetAmsNet,
            targetAmsPort: Int = default._targetAmsPort,
            sourceAmsNetId: String = default._sourceAmsNet,
            sourceAmsPort: Int = default._sourceAmsPort,
            adapterDeviceID: String = default._adapterDeviceID,
            name: String = default._name,
            description: String = default._description,
            protocolAdapter: String? = default._protocolAdapterID
        ): AdsSourceConfiguration {


            val instance = createSourceConfiguration<AdsSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter
            )

            with(instance) {
                _adapterDeviceID = adapterDeviceID
                _channels = channels
                _targetAmsNet = targetAmsNetId
                _targetAmsPort = targetAmsPort
                _sourceAmsNet = sourceAmsNetId
                _sourceAmsPort = sourceAmsPort
            }
            return instance
        }
    }

}
