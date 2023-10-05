/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

/**
 * Modbus device source channel configuration
 */
@ConfigurationClass
class ModbusChannelConfiguration : ChannelConfiguration() {

    @SerializedName(CONFIG_TYPE)
    private var _type: ModbusChannelType? = null

    /**
     * Type of the modbus channel
     * @see ModbusChannelType
     */
    val type: ModbusChannelType?
        get() = _type

    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = "0"

    /**
     * Start address to read from
     */
    val address: Short
        get() {
            return try {
                _address.toShort()
            } catch (_: NumberFormatException) {
                0
            }
        }

    @SerializedName(CONFIG_SIZE)
    private var _size: Short = 1

    /**
     * number of values to read
     */
    val size: Short
        get() = _size

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateType()
        validateAddress()
        validateSize()
        validated = true
    }

    // Validates size
    private fun validateSize() =
        ConfigurationException.check(
            (size > 0),
            "Modbus channel $CONFIG_SIZE must be 1 or higher",
            CONFIG_SIZE,
            this
        )


    // Validates address
    private fun validateAddress() =
        ConfigurationException.check(
            (address > 0),
            "Modbus $CONFIG_ADDRESS must be 0 or higher",
            CONFIG_ADDRESS,
            this
        )

    // Validates modbus channel type
    private fun validateType() =
        ConfigurationException.check(
            ((type != null) && (type in ModbusChannelType.values())),
            "Channel $CONFIG_TYPE must be one of ${ModbusChannelType.values().joinToString()}",
            CONFIG_TYPE,
            this
        )

    companion object {
        private const val CONFIG_TYPE = "Type"
        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_SIZE = "Size"

        private val default = ModbusChannelConfiguration()

        fun create(type: ModbusChannelType? = default._type,
                   address: String = default._address,
                   size: Short = default._size,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): ModbusChannelConfiguration {

            val instance = createChannelConfiguration<ModbusChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )
            with(instance) {
                _type = type
                _address = address
                _size = size
            }
            return instance
        }

    }

}