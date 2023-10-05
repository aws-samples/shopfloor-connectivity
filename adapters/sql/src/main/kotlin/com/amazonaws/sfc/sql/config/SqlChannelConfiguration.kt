/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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








