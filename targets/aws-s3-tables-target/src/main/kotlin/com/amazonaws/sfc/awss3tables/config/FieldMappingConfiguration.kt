// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awss3tables.config.FieldConfiguration.Companion.CONFIG_COLUMN_MAPPING
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.JmesPathExtended
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression

@ConfigurationClass
class FieldMappingConfiguration : Validate {

    @SerializedName(CONFIG_VALUE_QUERY)
    private var _valueQuery: String = ""

    val valueQueryStr: String
        get() {
            return _valueQuery
        }

    val valueQuery: Expression<Any>?
        get() {
            return getExpression(_valueQuery)
        }

    @SerializedName(CONFIG_TRANSFORMATION)
    private var _transformationID: String? = null
    val transformationID: String?
        get() {

            return _transformationID
        }

    //  @SerializedName(CONFIG_COLUMN_MAPPING)
    private var _subMappings: Map<String, FieldMappingConfiguration>? = null
    val subMappings: Map<String, FieldMappingConfiguration>
        get() {
            return _subMappings ?: emptyMap()
        }


    fun getExpression(path: String?): Expression<Any>? {
        if (path.isNullOrEmpty()) {
            return null
        }

        val p: String = JmesPathExtended.escapeJMesString(path)
        if (!cachedJmespathQueries.containsKey(p)) {
            cachedJmespathQueries[p] = try {
                jmespath.compile(if (p.startsWith("@.")) p else "@.$p")
            } catch (e: Throwable) {
                null
            }
        }
        return cachedJmespathQueries[p]
    }

    private val jmespath by lazy {
        JmesPathExtended.create()
    }

    // Caching compiled JMESPath queries
    private val cachedJmespathQueries = mutableMapOf<String, Expression<Any>?>()


    private var _validated = false
    override fun validate() {
        if (validated) return
        validateValueQuery()
        validateSubMappings()
        validated = true
    }

    private fun validateSubMappings() {
        if (_subMappings == null) return
        if (_subMappings!!.isEmpty()) throw ConfigurationException("Sub mappings can not be empty", CONFIG_COLUMN_MAPPING, this)

        if (_valueQuery.isNotEmpty()) throw ConfigurationException("Sub mappings can not be used with value query", CONFIG_COLUMN_MAPPING, this)
        if (_transformationID != null) throw ConfigurationException("Sub mappings can not be used with transformation", CONFIG_COLUMN_MAPPING, this)

        _subMappings!!.forEach { (key, value) ->
            value.validate()
        }
    }

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    private fun validateValueQuery() {

        if (_subMappings != null) {
            return
        }

        ConfigurationException.check(
            (_valueQuery.isNotEmpty()),
            "$CONFIG_VALUE_QUERY must be specified",
            CONFIG_VALUE_QUERY,
            this
        )

        ConfigurationException.check(
            (valueQuery != null),
            "$CONFIG_VALUE_QUERY  \"$_valueQuery\" is not a valid JmesPath expression",
            CONFIG_VALUE_QUERY,
            this
        )
    }


    companion object {

        private const val CONFIG_VALUE_QUERY = "ValueQuery"

        private val default = FieldMappingConfiguration()

        fun create(valueQuery: String = default._valueQuery,
                   transformation: String? = default._transformationID): FieldMappingConfiguration {
            val instance = FieldMappingConfiguration()

            with(instance) {
                _valueQuery = valueQuery
                _transformationID = transformation
            }
            return instance
        }

        fun fromMap(map: Map<*, *>): FieldMappingConfiguration {
            val instance = FieldMappingConfiguration()

            with(instance) {
                _valueQuery = map[CONFIG_VALUE_QUERY].toString()
                _transformationID = map[CONFIG_TRANSFORMATION]?.toString()
            }
            return instance
        }

        fun fromJson(jsonObject: JsonObject?): FieldMappingConfiguration? {

            if (jsonObject == null) return null

            if (jsonObject.isJsonObject) {
                return if (jsonObject.has(CONFIG_COLUMN_MAPPING)) {
                    buildSubMappinfg(jsonObject)
                } else {
                    buildMapping(jsonObject)
                }
            } else {
                throw ConfigurationException("Invalid JSON object, mapping must be a valid mapping or a list of sub mappings", CONFIG_COLUMN_MAPPING, jsonObject.toString())
            }


        }

        private fun buildMapping(jsonObject: JsonObject): FieldMappingConfiguration {
            val instance = FieldMappingConfiguration()
            val jsonQuery = jsonObject.get(CONFIG_VALUE_QUERY) ?: throw ConfigurationException("${CONFIG_VALUE_QUERY} must be set", CONFIG_VALUE_QUERY, jsonObject.toString())
            instance._valueQuery = if (jsonQuery.isJsonPrimitive) {
                jsonQuery.asString
            } else throw ConfigurationException("$CONFIG_VALUE_QUERY must be a string", CONFIG_VALUE_QUERY, jsonQuery.toString())
            val jsonTransformation = jsonObject.get(CONFIG_TRANSFORMATION)
            if (jsonTransformation != null) {
                instance._transformationID = if (jsonTransformation.isJsonPrimitive) {
                    jsonTransformation.asString
                } else throw ConfigurationException(
                    "${CONFIG_TRANSFORMATION} must be a string",
                    CONFIG_TRANSFORMATION,
                    jsonObject.toString())
            }
            return instance
        }

        private fun buildSubMappinfg(jsonObject: JsonObject): FieldMappingConfiguration {
            val instance = FieldMappingConfiguration()
            val jsonMapping = jsonObject.get(CONFIG_COLUMN_MAPPING).asJsonObject
            if (jsonMapping.isJsonObject) {
                instance._subMappings = sequence {
                    jsonMapping.keySet().forEach { key ->
                        val subMapping = fromJson(jsonMapping.get(key).asJsonObject)
                        if (subMapping != null) {
                            yield(key to subMapping)
                        }
                    }
                }.toMap()
                if (instance._subMappings?.isEmpty() == true) throw ConfigurationException(
                    "Sub mappings CONFIG_COLUMN_MAPPING can not be empty",
                    CONFIG_COLUMN_MAPPING,
                    jsonObject.toString())


            } else throw ConfigurationException("Invalid JSON object, mapping of sub fields must be a JSON object", CONFIG_COLUMN_MAPPING, jsonObject.toString())
            return instance
        }

        private fun FieldMappingConfiguration.buildMapping(jsonObject: JsonObject) {
            val jsonQuery = jsonObject.get(CONFIG_VALUE_QUERY) ?: throw ConfigurationException("$CONFIG_VALUE_QUERY must be set", CONFIG_VALUE_QUERY, jsonObject.toString())

            _valueQuery = if (jsonQuery.isJsonPrimitive) {
                jsonQuery.asString
            } else throw ConfigurationException("$CONFIG_VALUE_QUERY must be a string", CONFIG_VALUE_QUERY, jsonQuery.toString())

            val jsonTransformation = jsonObject.get(CONFIG_TRANSFORMATION)
            if (jsonTransformation != null) {
                _transformationID = if (jsonTransformation.isJsonPrimitive) {
                    jsonTransformation.asString
                } else throw ConfigurationException("$CONFIG_TRANSFORMATION must be a string", CONFIG_TRANSFORMATION, jsonObject.toString())
            }
        }
    }
}