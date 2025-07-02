// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper.Companion.buildPartitionSpecification
import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper.Companion.buildSchema
import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper.Companion.validateName
import com.amazonaws.sfc.awss3tables.config.ColumnConfiguration.Companion.CONFIG_COLUMN_MAPPING
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

class TableConfiguration : Validate{
    @SerializedName(CONFIG_TABLE_NAME)
    var _tableName: String? = null
    val tableName: String
        get() = _tableName ?: ""

    @SerializedName(CONFIG_PARTITION)
    val _partition: TablePartitionConfiguration? = null
    val partition: TablePartitionConfiguration?
        get() = _partition

    @SerializedName(CONFIG_SCHEMA)
    var _schema: TableSchemaConfiguration = emptyList()
    val schema: TableSchemaConfiguration
        get() = _schema

    @SerializedName(CONFIG_PARTITION_OPTIMIZED)
    var _partitionOptimized: Boolean = true
    val partitionOptimized: Boolean
        get() = _partitionOptimized


    val catalogSchema by lazy{
        buildSchema(schema)
    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @SerializedName(CONFIG_COLUMN_MAPPING)
    private var _mappings: List<Map<String, ColumnMappingConfiguration>> = emptyList()
    val mappings: List<Map<String, ColumnMappingConfiguration>>
        get() = _mappings

    override fun validate() {
        if (validated) return
        validateSchema()
        validateTableName()
        validatePartition()
        validateMappings()

        validated = true

    }

    private fun validateMappings() {
        ConfigurationException.check(
            !_mappings.isEmpty(),
            "$CONFIG_COLUMN_MAPPING can not be empty",
            CONFIG_COLUMN_MAPPING,
            null
        )
        _mappings.forEach { mapping ->
            mapping.values.forEach { fieldConfig -> fieldConfig.validate() }
        }
    }


    private fun validateTableName() {
        ConfigurationException.check(
            !(_tableName.isNullOrEmpty()),
            "$CONFIG_TABLE_NAME must be specified",
            CONFIG_TABLE_NAME,
            this
        )
        val (namespaceIsValid, reason) = validateName(_tableName!!)
        if (!namespaceIsValid) {
            throw ConfigurationException("Invalid $CONFIG_TABLE_NAME \"$_tableName\", $reason", CONFIG_TABLE_NAME, null)
        }
    }

    private fun validateSchema() {
        ConfigurationException.check(
            !(_schema.isEmpty()),
            "$CONFIG_SCHEMA must be specified",
            CONFIG_SCHEMA,
            null
        )
        try{
            buildSchema(schema)
        } catch( e : Exception){
            throw ConfigurationException("Invalid $CONFIG_SCHEMA, ${e.message}", CONFIG_SCHEMA, null)
        }
    }


    private fun validatePartition() {
        if (_partition == null) return
        try{
            buildPartitionSpecification( buildSchema(schema), _partition)
        }catch (e: Exception){
            throw ConfigurationException("Invalid $CONFIG_PARTITION, ${e.message}", CONFIG_PARTITION, null)
        }
    }

    companion object{
        private const val CONFIG_TABLE_NAME = "TableName"
        private const val CONFIG_PARTITION = "Partition"
        private const val CONFIG_SCHEMA = "Schema"
        private const val CONFIG_PARTITION_OPTIMIZED = "PartitionOptimization"

    }
}