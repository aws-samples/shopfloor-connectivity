
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class SqlChannelConfiguration : ChannelConfiguration() {

    enum class SqlChannelValueType {
        SINGLE_COLUMN,
        MULTIPLE_COLUMNS,
        ALL_COLUMNS
    }

    @SerializedName(CONFIG_COLUMN_NAMES)
    private var _columnNames: List<String> = ALL_ROW_COLUMNS

    val columnNames: List<String>
        get() = _columnNames

    override fun validate() {
        validated = true
    }


    val sqlValueType by lazy {
        when {
            (_columnNames == ALL_ROW_COLUMNS) -> SqlChannelValueType.ALL_COLUMNS
            (_columnNames.size == 1 && _columnNames[0] != SQL_WILDCARD) -> SqlChannelValueType.SINGLE_COLUMN
            else -> SqlChannelValueType.MULTIPLE_COLUMNS
        }
    }

    companion object {

        private const val CONFIG_COLUMN_NAMES = "ColumnNames"
        private const val SQL_WILDCARD = "*"
        val ALL_ROW_COLUMNS = listOf(SQL_WILDCARD)

        private val default = SqlChannelConfiguration()

        fun create(columnNames: List<String> = default._columnNames,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): SqlChannelConfiguration {

            val instance = createChannelConfiguration<SqlChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _columnNames = columnNames
            }
            return instance
        }

    }


}








