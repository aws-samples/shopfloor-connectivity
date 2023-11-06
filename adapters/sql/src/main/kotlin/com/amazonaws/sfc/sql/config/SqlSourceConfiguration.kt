
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SqlSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_DB_SERVER)
    private var _adapterDbServerID: String = ""

    val adapterDbServerID: String
        get() = _adapterDbServerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, SqlChannelConfiguration>()
    val channels: Map<String, SqlChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @SerializedName(CONFIG_SOURCE_READ_SQL_STATEMENT)
    private val _sqlReadStatement: String = ""
    val sqlReadStatement: String
        get() = _sqlReadStatement

    @SerializedName(CONFIG_SOURCE_SQL_READ_PARAMS)
    private val _sqlReadParameters = listOf<Any>()
    val sqlReadParameters
        get() = _sqlReadParameters

    @SerializedName(CONFIG_SOURCE_SINGLE_ROW)
    private val _singleRow: Boolean = false

    val singleRow: Boolean
        get() = _singleRow

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        validateMustHaveDbServer()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validateMustHaveSqlReadStatement()
        validateSqlReadParameters()

        validated = true
    }

    // Server must be set
    private fun validateMustHaveDbServer() =
        ConfigurationException.check(
            (_adapterDbServerID.isNotEmpty()),
            "$CONFIG_ADAPTER_DB_SERVER for SQL source must be set",
            CONFIG_ADAPTER_DB_SERVER,
            this
        )

    private fun validateMustHaveSqlReadStatement() =
        ConfigurationException.check(
            (_sqlReadStatement.isNotEmpty()),
            "$CONFIG_SOURCE_READ_SQL_STATEMENT for SQL source must be set",
            CONFIG_SOURCE_READ_SQL_STATEMENT,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "Sql source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    private fun validateSqlReadParameters() {
        val parametersInStatement = _sqlReadStatement.count { it == '?' }
        ConfigurationException.check(
            parametersInStatement == _sqlReadParameters.size,
            "Number of parameters in $CONFIG_SOURCE_SQL_READ_PARAMS does not match number of parameters ($parametersInStatement) \"$sqlReadStatement\"",
            CONFIG_SOURCE_SQL_READ_PARAMS,
            this)
    }

    companion object {
        const val CONFIG_ADAPTER_DB_SERVER = "AdapterDbServer"
        private const val CONFIG_SOURCE_READ_SQL_STATEMENT = "SqlReadStatement"
        private const val CONFIG_SOURCE_SINGLE_ROW = "SingleRow"
        const val CONFIG_SOURCE_SQL_READ_PARAMS = "SqlReadParameters"

        private val default = SqlSourceConfiguration()

        fun create(channels: Map<String, SqlChannelConfiguration> = default._channels,
                   adapterDbServerID: String = default._adapterDbServerID,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): SqlSourceConfiguration {

            val instance = createSourceConfiguration<SqlSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _adapterDbServerID = adapterDbServerID
                _channels = channels
            }
            return instance
        }
    }


}
