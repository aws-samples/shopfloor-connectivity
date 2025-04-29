// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.j1939.protocol.isNumeric
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class J1939ChannelConfiguration : ChannelConfiguration() {

    @SerializedName(value = CONFIG_PGN, alternate = [CONFIG_PGN_UPPERCASE])
    private var _pgn: String = ""

    val pgn: String
        get() = if (isNumeric(_pgn))_pgn.split('.').first() else _pgn

    @SerializedName(value = CONFIG_SPN, alternate = [CONFIG_SPN_UPPERCASE])
    private var _spnList: String? = null


    val spnList: List<String>?
        get() = _spnList?.split(',')?.map { it.trim() }?.map{if (isNumeric(it))it.split('.').first() else it}

    @SerializedName(CONFIG_RAW_FORMAT)
    private var _rawFormat : J1939RawFormat? = null
    val rawFormat: J1939RawFormat?
        get() = _rawFormat

    override fun validate() {
        if (validated) return
        super.validate()
        validated = true
    }


    private fun validatePgn() {
        ConfigurationException.check(
            pgn.isNotEmpty(),
            "$CONFIG_PGN ofJ1939 Channel Configuration requires a PGN",
            CONFIG_PGN,
            this
        )
    }

    companion object {

        private const val CONFIG_PGN = "Pgn"
        private const val CONFIG_PGN_UPPERCASE = "PGN"

        private const val CONFIG_SPN = "Spn"
        private const val CONFIG_SPN_UPPERCASE = "SPN"

        internal const val CONFIG_RAW_FORMAT = "RawFormat"

        private val default = J1939ChannelConfiguration()

        fun create(pgn: String = default._pgn,
                   spn: String? = default._spnList,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID,
                   conditionFilter: String? = default._conditionFilterID): J1939ChannelConfiguration {

            val instance = createChannelConfiguration<J1939ChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter,
                conditionFilter = conditionFilter
            )

            with(instance) {
                _pgn = pgn
                _spnList = spn
            }
            return instance
        }

    }
}








