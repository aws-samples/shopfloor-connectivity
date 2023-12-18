// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.csvfile.sfc.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class CsvFileChannelConfiguration : ChannelConfiguration() {


    @SerializedName(CONFIG_COL_NAME)
    private var _colName: String? = ""

    val colName: String?
        get() = _colName?.toString()

    private fun validateColName() {
        ConfigurationException.check(
            !colName.isNullOrEmpty(),
            "$CONFIG_COL_NAME can not be empty",
            CONFIG_COL_NAME,
            this
        )
    }

    @SerializedName(CONFIG_COL_INDEX)
    private var _colIdx: Int = -1
    val colIdx: Int
        get() = _colIdx

    private fun validateColIdx() {
        ConfigurationException.check(
            (colIdx > -1),
            "$CONFIG_COL_INDEX can not be empty and must be set to >= 0",
            CONFIG_COL_INDEX,
            this
        )
    }

    override fun validate() {
        if (validated) return
        super.validate()
        validateColName()
        validateColIdx()
        validated = true
    }

    companion object {
        private val DEFAULT_COL_INDEX = null
        private const val CONFIG_COL_NAME = "ColName"
        private const val CONFIG_COL_INDEX = "ColIndex"

        private val default = CsvFileChannelConfiguration()

        fun create(
            colName: String? = default._colName,
            colIdx: Int = default._colIdx,
            name: String? = default._name,
            description: String = default._description,
            transformation: String? = default._transformationID,
            metadata: Map<String, String> = default._metadata,
            changeFilter: String? = default._changeFilterID,
            valueFilter: String? = default._valueFilterID): CsvFileChannelConfiguration {

            val instance = createChannelConfiguration<CsvFileChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _colName = colName
                _colIdx = colIdx
            }
            return instance
        }
    }
}








