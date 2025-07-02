// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.apache.iceberg.types.Types
import org.apache.iceberg.types.Types.ListType.ofOptional
import org.apache.iceberg.types.Types.ListType.ofRequired
import org.apache.iceberg.types.Types.MapType.ofOptional
import org.apache.iceberg.types.Types.MapType.ofRequired
import org.apache.iceberg.types.Types.fromPrimitiveString

@ConfigurationClass
class ColumnConfiguration() : Validate {

    var _id: Int = 0
    val id: Int
        get() {
            return _id
        }

    var field: Types.NestedField? = null

    @SerializedName(CONFIG_COLUMN_NAME)
    private var _name: String? = null
    val name: String
        get() = _name ?: ""


    @SerializedName(CONFIG_COLUMN_TYPE)
    private var _type: org.apache.iceberg.types.Type? = null
    val type: org.apache.iceberg.types.Type?
        // https://docs.aws.amazon.com/athena/latest/ug/querying-iceberg-supported-data-types.html
        get() = _type

    @SerializedName(CONFIG_COLUMN_OPTIONAL)
    private var _optional: Boolean = true
    val optional: Boolean
        get() = _optional

    var _subFields: TableSchemaConfiguration = emptyList()
    val subFields: TableSchemaConfiguration
        get() = _subFields

    private var _validated = false
    override fun validate() {
        if (validated) return
        validateColumnName()
        validateDataType()
        validated = true
    }

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }


    private fun validateColumnName() {
        ConfigurationException.check(
            _name != null,
            "$CONFIG_COLUMN_NAME cannot be empty",
            CONFIG_COLUMN_NAME,
            this
        )
    }

    private fun validateDataType() {
        ConfigurationException.check(
            _type != null,
            "$CONFIG_COLUMN_TYPE cannot be null",
            CONFIG_COLUMN_TYPE,
            this
        )
    }

    companion object {

        private const val CONFIG_COLUMN_NAME = "Name"
        const val CONFIG_COLUMN_TYPE = "Type"
        private const val CONFIG_COLUMN_OPTIONAL = "Optional"
        const val CONFIG_COLUMN_MAPPING = "Mappings"
        private val default = ColumnConfiguration()

        fun create(
            name: String = default.name,
            type: org.apache.iceberg.types.Type? = default._type,
            optional: Boolean = default.optional): ColumnConfiguration {
            val instance = ColumnConfiguration()

            with(instance) {
                _name = name
                _type = type
                _optional = optional

            }
            return instance
        }

        fun fromJson(jsonObject: JsonObject): ColumnConfiguration {
            val instance = ColumnConfiguration()
            with(instance) {

                _id = AwsS3TablesHelper.nextId()

                val jsonName = jsonObject.get(CONFIG_COLUMN_NAME)
                _name = if (jsonName != null && jsonName.isJsonPrimitive) jsonName.asString else ""

                val jsonOptional = jsonObject.get(CONFIG_COLUMN_OPTIONAL)
                _optional = if (jsonOptional != null && jsonOptional.isJsonPrimitive)
                    try {
                        jsonOptional.asBoolean
                    } catch (_: Exception) {
                        false
                    } else true

                val jsonType = jsonObject.get(CONFIG_COLUMN_TYPE)
                field = if (jsonType != null) {

                    if (jsonType.isJsonPrimitive) {

                        when {
                            REGEX_LIST.matches(jsonType.asString.lowercase()) -> buildListField(jsonType)
                            REGEX_MAP.matches(jsonType.asString.lowercase()) -> buildMapField(jsonType)
                            else -> buildPrimitiveField(jsonType)
                        }
                    } else if (jsonType.isJsonArray) buildStructField(jsonType)
                    else throw ConfigurationException("$CONFIG_COLUMN_TYPE must be a list of nested fields of the structure", CONFIG_COLUMN_TYPE, jsonType.toString())

                } else {
                    throw ConfigurationException("$CONFIG_COLUMN_TYPE cannot be empty", CONFIG_COLUMN_TYPE, jsonType.toString())
                }


                return instance
            }

        }

        private fun ColumnConfiguration.buildStructField(jsonType: JsonElement): Types.NestedField? {

            _subFields = sequence {
                (jsonType as JsonArray).map {
                    if (it.isJsonObject) {
                        val jsonField = it.asJsonObject
                        yield(fromJson(jsonField))
                    }
                }
            }.toList()

            _type = Types.StructType.of(
                _subFields.map {
                    it.field
                }
            )
            return buildField()
        }

        private fun ColumnConfiguration.buildPrimitiveField(jsonType: JsonElement): Types.NestedField? = try {
            _type = fromPrimitiveString(jsonType.asString.lowercase())
            buildField()
        } catch (e: Exception) {
            throw ConfigurationException("Unsupported $CONFIG_COLUMN_TYPE : \"${jsonType.asString}\". $e", CONFIG_COLUMN_TYPE, jsonType.asString)
        }

        private fun ColumnConfiguration.buildMapField(jsonType: JsonElement): Types.NestedField? {
            val match = REGEX_MAP.find(jsonType.asString)!!
            val keyType = fromPrimitiveString(match.groupValues[1])
            val valueType = fromPrimitiveString(match.groupValues[2])
            val valueId = AwsS3TablesHelper.nextId()
            val keyId = AwsS3TablesHelper.nextId()
            _type = if (_optional) {
                ofOptional(keyId, valueId, keyType, valueType)
            } else {
                ofRequired(keyId, valueId, keyType, valueType)
            }
            return buildField()
        }

        private fun ColumnConfiguration.buildListField(jsonType: JsonElement): Types.NestedField? {
            val match = REGEX_LIST.find(jsonType.asString)
            val elementType = fromPrimitiveString(match!!.groupValues[1].lowercase())
            val elementId = AwsS3TablesHelper.nextId()
            _type = if (_optional) {
                ofOptional(elementId, elementType)

            } else {
                ofRequired(elementId, elementType)
            }
            return buildField()
        }

        private fun ColumnConfiguration.buildField(): Types.NestedField? = if (optional) {
            Types.NestedField.optional(_id, name, type)
        } else {
            Types.NestedField.required(_id, name, type)
        }


        private val REGEX_LIST = Regex("^list<(.+)>$")
        private val REGEX_MAP = Regex("^map<(.+),(.+)>$")
    }

}