// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.slmp.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.slmp.protocol.SlmpAccessPoint
import com.amazonaws.sfc.slmp.protocol.SlmpDeviceItem
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class SlmpChannelConfiguration : ChannelConfiguration() {


    @SerializedName(CONFIG_ACCESS_POINT)
    private var _accessPoint: String = ""
    val accessPoint: SlmpAccessPoint by lazy {
        SlmpAccessPoint.fromString(_accessPoint)
    }

    @SerializedName(CONFIG_DATA_TYPE)
    private var _dataType: String? = null

    val deviceItem: SlmpDeviceItem by lazy {
        SlmpDeviceItem.build(accessPoint, _dataType, _size)
    }

    @SerializedName(CONFIG_FORCE_DEVICE_READ)
    private var _forceDeviceRead: Boolean = false
    val forceDeviceRead: Boolean
        get() = _forceDeviceRead

    @SerializedName(CONFIG_SIZE)
    private var _size: Int? = null


    override fun validate() {
        validateAccessPoint()
        validateSize()
        validateDeviceItem()
    }

    private fun validateDeviceItem() {
        val item = try {
            this.deviceItem
        } catch (e: Exception) {
            throw ConfigurationException("Invalid configuration for SLMP channel, ", CONFIG_CHANNELS, this)
        }
        if (_size != null && _size != item.size) {
            throw ConfigurationException(
                "Size is defined in both type \"$_dataType\" and $CONFIG_SIZE and have different values, use either $CONFIG_SIZE or size in $CONFIG_DATA_TYPE or make sure the size do match",
                "$CONFIG_SIZE, $CONFIG_DATA_TYPE", this
            )
        }
    }

    private fun validateSize() {
        ConfigurationException.check(
            _size == null || _size!! > 0,
            "Size must be 1 or more",
            CONFIG_SIZE,
            this
        )
    }


    private fun validateAccessPoint() {
        ConfigurationException.check(
            _accessPoint.isNotEmpty(),
            "$CONFIG_ACCESS_POINT of SLMP channel can not be empty",
            CONFIG_ACCESS_POINT, this
        )

        try {
            SlmpAccessPoint.fromString(_accessPoint)
        } catch(e : Exception){
            throw ConfigurationException("Invalid $CONFIG_ACCESS_POINT \"$_accessPoint\" of SLMP channel, $e", CONFIG_ACCESS_POINT, this)
        }
        validated = true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlmpChannelConfiguration

        if (_accessPoint != other._accessPoint) return false
        if (_dataType != other._dataType) return false
        if (_forceDeviceRead != other._forceDeviceRead) return false
        if (_size != other._size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _accessPoint.hashCode()
        result = 31 * result + (_dataType?.hashCode() ?: 0)
        result = 31 * result + _forceDeviceRead.hashCode()
        result = 31 * result + (_size ?: 0)
        return result
    }

    companion object {

        private const val CONFIG_ACCESS_POINT = "AccessPoint"
        private const val CONFIG_DATA_TYPE = "DataType"
        private const val CONFIG_SIZE = "Size"
        private const val CONFIG_FORCE_DEVICE_READ = "ForceDeviceRead"

        private val default = SlmpChannelConfiguration()

        fun create(
            accessPoint: String = default._accessPoint,
            dataType: String? = default._dataType,
            size: Int? = default._size,
            name: String? = default._name,
            description: String = default._description,
            transformation: String? = default._transformationID,
            metadata: Map<String, String> = default._metadata,
            changeFilter: String? = default._changeFilterID,
            valueFilter: String? = default._valueFilterID,
            conditionFilter: String? = default._conditionFilterID
        ): SlmpChannelConfiguration {

            val instance = createChannelConfiguration<SlmpChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter,
                conditionFilter = conditionFilter
            )

            with(instance) {
                _accessPoint = accessPoint
                _dataType = dataType
                _size = size
            }
            return instance
        }

    }
}








