// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.opcuatarget.OpcuaServerDataTypes
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.UaRuntimeException
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import java.time.Instant

class VariableNodeConfiguration(configNode: BaseNodeConfiguration,
                                dataType: OpcuaServerDataTypes,
                                val initValue: Any?,
                                val arrayDimensions: List<Int>?,
                                valueSelector: String? = null,
                                timestampSelector: String? = null,
                                val transformationID: String? = null) :
        BaseNodeConfiguration(
            configNode.nameSpaceIndex,
            configNode.id,
            configNode.browseName,
            configNode.displayName,
            configNode.description) {

    constructor(namespaceIndex: Int,
                id: String, browseName: String? = null,
                displayName: String? = null,
                description: String? = null,
                dataType: OpcuaServerDataTypes,
                initValue: Any? = null,
                arrayDimensions: List<Int>? = null,
                valueSelector: String? = null,
                timestampSelector: String? = null,
                transformation: String? = null) :
            this(BaseNodeConfiguration(namespaceIndex, id, browseName, displayName, description), dataType, initValue, arrayDimensions, valueSelector, timestampSelector, transformation)

    private val _dataType = dataType
    val dataType
        get() = _dataType
    val dataTypeIdentifier: NodeId? by lazy {
        _dataType.identifier
    }

    override fun validate() {
        if (validated) return

        super.validate()

        validateValueSelector()
        validateTimestampSelector()

        validated = true
    }

    private fun validateTimestampSelector() {
        if (_timestampSelector != null) {
            try {
                JmesPathExtended.create().compile(JmesPathExtended.escapeJMesString(_timestampSelector))
            } catch (e: Exception) {
                throw ConfigurationException("Invalid selector \"$_timestampSelector\" for variable ${this.id}, ${e.message}", CONFIG_TIMESTAMP_SELECTOR, this)
            }
        }
    }

    private fun validateValueSelector() {
        if (_valueSelector != null) {
            try {
                JmesPathExtended.create().compile(JmesPathExtended.escapeJMesString(_valueSelector))
            } catch (e: Exception) {
                throw ConfigurationException("Invalid selector \"$_valueSelector\" for variable ${this.id}, ${e.message}", CONFIG_VALUE_SELECTOR, this)
            }
        }
    }

    private val _valueSelector: String? = valueSelector
    val valueSelector
        get() = _valueSelector


    private val _timestampSelector: String? = timestampSelector
    val timestampSelector
        get() = _timestampSelector

    companion object {

        private const val CONFIG_NODE_INITIAL_VALUE = "InitValue"
        private const val CONFIG_NODE_DATA_TYPE = "DataType"
        private const val CONFIG_NODE_ARRAY_DIMENSIONS = "ArrayDimensions"
        private const val CONFIG_VALUE_SELECTOR = "ValueQuery"
        private const val CONFIG_TIMESTAMP_SELECTOR = "TimestampQuery"

        fun getValue(jsonElement: JsonElement?, dataType: OpcuaServerDataTypes): Any? {

            if (jsonElement == null) return null

            var json: JsonElement? = null
            if (jsonElement is JsonPrimitive)
                json = jsonElement
            else {
                if (jsonElement.isJsonArray) return jsonElement.asJsonArray.map { getValue(it, dataType) }
                if (jsonElement.isJsonObject) json = jsonElement.asJsonObject?.get(CONFIG_NODE_INITIAL_VALUE)
                return getValue(json, dataType)
            }

            val dataTypeIdentifier = dataType.identifier
            val value = try {
                when (dataTypeIdentifier) {
                    Identifiers.Boolean -> json.asBoolean
                    Identifiers.SByte -> json.asByte
                    Identifiers.ExpandedNodeId -> ExpandedNodeId.parse(json.asString)
                    Identifiers.Float -> json.asFloat
                    Identifiers.Int32 -> json.asInt
                    Identifiers.LocalizedText -> json.asString
                    Identifiers.Int64 -> json.asLong
                    Identifiers.NodeId -> NodeId.parse(json.asString)
                    Identifiers.QualifiedName -> json.asString
                    Identifiers.Int16 -> json.asShort
                    Identifiers.String -> json.asString
                    Identifiers.UInt32 -> json.asInt
                    Identifiers.UInt64 -> json.asLong
                    Identifiers.UInt16 -> json.asShort
                    Identifiers.XmlElement -> json.asString
                    Identifiers.Byte -> json.asByte
                    Identifiers.Double -> json.asDouble
                    Identifiers.Guid -> json.asString
                    Identifiers.ByteString -> json.asString
                    Identifiers.DateTime -> DateTime(Instant.parse(json.asString))
                    else -> null
                }
            } catch (e: ClassCastException) {
                null
            } catch (e: IllegalStateException) {
                null
            } catch (e: UaRuntimeException) {
                null
            }
            return value
        }

        fun fromJson(key: String, json: JsonObject): VariableNodeConfiguration {
            val configNode = BaseNodeConfiguration.fromJson(key, json.asJsonObject.getAsJsonObject(key))

            val dataTypeStr = json.asJsonObject[key]?.asJsonObject?.get(CONFIG_NODE_DATA_TYPE)?.asString ?: ""
            val dataType = OpcuaServerDataTypes.fromString(dataTypeStr)

            val arrayDimensions = getArrayDimensions(json.asJsonObject[key])

            var initValue = getValue(json.asJsonObject[key], dataType)

            if (initValue != null && arrayDimensions != null) {
                if (!validateDimension(initValue, arrayDimensions)) initValue = null
            }

            val valueSelector = json.asJsonObject[key]?.asJsonObject?.get(CONFIG_VALUE_SELECTOR)?.asString
            val timestampSelector = json.asJsonObject[key]?.asJsonObject?.get(CONFIG_TIMESTAMP_SELECTOR)?.asString
            val transformationID = json.asJsonObject[key]?.asJsonObject?.get(CONFIG_TRANSFORMATION)?.asString ?: ""


            val variableNode = VariableNodeConfiguration(configNode, dataType, initValue, arrayDimensions, valueSelector, timestampSelector, transformationID)
            return variableNode
        }

        private fun getArrayDimensions(jsonElement: JsonElement?): List<Int>? {
            val json = jsonElement?.asJsonObject?.get(CONFIG_NODE_ARRAY_DIMENSIONS) ?: return null
            if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
                return IntArray(1).apply { this[0] = json.asInt }.toList()
            }
            if (!json.isJsonArray) {
                val arrayDimensions = json.asJsonArray
                val d = arrayDimensions.map {
                    if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) {
                        it.asInt
                    } else {
                        return null
                    }
                }
                return d.toIntArray().toList()
            }
            val arrayDimensions = json.asJsonArray
            val dimensions = IntArray(arrayDimensions.size())
            for (i in 0 until arrayDimensions.size()) {
                dimensions[i] = arrayDimensions[i].asInt
            }
            return dimensions.asList()
        }

        private fun validateDimension(i: Any?, d: List<Int>): Boolean {
            if (i == null || i !is List<*>) return false
            val expectedSize = d.first()
            if (expectedSize != i.size) return false
            if (d.size > 1) {
                if (i.any { it !is List<*> })
                    return false
                return i.all { validateDimension(it as List<*>, d.drop(1)) }
            }
            return true

        }
    }

}