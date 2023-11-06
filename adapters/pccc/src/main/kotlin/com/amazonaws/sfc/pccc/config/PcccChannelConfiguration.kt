
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.pccc.protocol.Address
import com.amazonaws.sfc.pccc.protocol.AddressException
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class PcccChannelConfiguration : ChannelConfiguration() {


    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = ""
    val address: Address by lazy { Address.parse(_address) }

    override fun validate() {

        if (_address.isEmpty()) {
            throw ConfigurationException("$CONFIG_ADDRESS of a PCCC channel can not be empty", CONFIG_ADDRESS, this)
        }

        try {
            address
        } catch (e: AddressException) {
            throw ConfigurationException("\"$_address\" is not a valid address, ${e.message}", CONFIG_ADDRESS, this)
        }

        validated = true
    }

    companion object {

        private const val CONFIG_ADDRESS = "Address"

        private val default = PcccChannelConfiguration()

        fun create(
            address: String = default._address,
            name: String? = default._name,
            description: String = default._description,
            transformation: String? = default._transformationID,
            metadata: Map<String, String> = default._metadata,
            changeFilter: String? = default._changeFilterID,
            valueFilter: String? = default._valueFilterID
        ): PcccChannelConfiguration {

            val instance = createChannelConfiguration<PcccChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _address = address
            }
            return instance
        }

    }
}








