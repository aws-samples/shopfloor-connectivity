
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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








