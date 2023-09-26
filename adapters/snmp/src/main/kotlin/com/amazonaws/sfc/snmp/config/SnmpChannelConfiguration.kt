/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.snmp.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName
import org.snmp4j.smi.OID


@ConfigurationClass
class SnmpChannelConfiguration : ChannelConfiguration() {

    @SerializedName(CONFIG_OID)
    private var _objectID: String? = null


    val objectID: String?
        get() = _objectID?.trim()


    override fun validate() {
        if (validated) return
        super.validate()
        validateOid()
        validated = true
    }


    private fun validateOid() {
        ConfigurationException.check(
            !objectID.isNullOrEmpty(),
            "$CONFIG_OID can not be empty",
            CONFIG_OID,
            this
        )

        ConfigurationException.check(
            oidSyntax.matches(objectID ?: ""),
            "$CONFIG_OID syntax \"$objectID\" is incorrect",
            CONFIG_OID,
            this
        )

        try {
            OID(objectID)
        } catch (e: RuntimeException) {
            throw ConfigurationException("Error parsing Object Identifier \"$objectID\", ${e.message}", CONFIG_OID, this)
        }
    }


    companion object {
        private const val CONFIG_OID = "ObjectId"

        private val oidSyntax = Regex("^(\\.\\d+)+$")

        private val default = SnmpChannelConfiguration()

        fun create(objectId: String? = default._objectID,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): SnmpChannelConfiguration {

            val instance = createChannelConfiguration<SnmpChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _objectID = objectId
            }
            return instance
        }
    }


}








