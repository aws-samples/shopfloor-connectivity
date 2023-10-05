/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.s7.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.ProtocolAdapterException
import com.google.gson.annotations.SerializedName
import org.apache.plc4x.java.s7.readwrite.field.S7Field

@ConfigurationClass
class S7FieldChannelConfiguration : ChannelConfiguration(), Validate {

    @SerializedName(CONFIG_S7_ADDRESS)
    private var _address: String = ""
    val address
        get() = _address

    val field: S7Field by lazy {
        S7Field.of(_address)
    }


    override fun validate() {

        if (validated) return
        super.validate()

        try {
            field

            if (field.memoryArea == null) {
                throw ProtocolAdapterException("Invalid memory area")
            }
        } catch (e: Exception) {
            throw ConfigurationException("\"$_address\" is not a valid S7 resource address, ${e.message}",
                CONFIG_S7_ADDRESS,
                this)
        }

        validated = true
    }

    companion object {

        private const val CONFIG_S7_ADDRESS = "Address"

        private val default = S7FieldChannelConfiguration()

        fun create(address: String = default._address,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): S7FieldChannelConfiguration {

            val instance = createChannelConfiguration<S7FieldChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter)

            with(instance) {
                _address = address
            }
            return instance
        }


    }
}