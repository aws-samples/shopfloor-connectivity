// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuawritetarget.config

import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.opcuawritetarget.OpcuaDataType
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId

class OpcuaNodeConfiguration : Validate {

    @SerializedName(CONFIG_NODE_ID)
    private var _nodeId: String? = null

    val nodeId: NodeId?
        get() {
            return if (_nodeId.isNullOrEmpty()) null else NodeId.parse(_nodeId!!)
        }

    @SerializedName(CONFIG_DATA_PATH)
    private var _dataPath: String? = null

    val dataPathStr: String?
        get() {
            return _dataPath
        }

    val dataPath: Expression<Any>?
        get() {
            return getExpression(_dataPath)
        }

    @SerializedName(CONFIG_DATA_TYPE)
    private var _dataType: String? = null
    val dataType: OpcuaDataType?
        get() = if (_dataType.isNullOrEmpty()) OpcuaDataType.UNDEFINED else OpcuaDataType.fromString(_dataType!!)

    @SerializedName(CONFIG_DIMENSIONS)
    private var _dimensions: List<Int> = emptyList()
    val dimensions: List<Int>
        get() = _dimensions

    @SerializedName(CONFIG_WARN_IF_NOT_PRESENT)
    private var _warnIfNotPresent: Boolean = true
    val warnIfNotPresent: Boolean
        get() = _warnIfNotPresent

    @SerializedName(CONFIG_TIMESTAMP_PATH)
    private var _timestampPath: String? = null

    val timestampPathStr: String?
        get() {
            return _timestampPath
        }

    val timestampPath: Expression<Any>?
        get() {
            return getExpression(_timestampPath)
        }

    @SerializedName(CONFIG_TRANSFORMATION)
    private var _transformation: String? = null
    val transformationID: String?
        get() = _transformation


    private var _validated: Boolean = false

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        validateDataPath()
        validateTimestampPath()
        validateDatatype()
    }

    private fun validateDatatype() {
        if (!_dataType.isNullOrEmpty()) {
            if (OpcuaDataType.entries.none { it.toString() == _dataType }) {
                throw ConfigurationException("Invalid data type $_dataType, valid types are ${OpcuaDataType.entries.joinToString { it.toString() }}", CONFIG_DATA_TYPE, this)
            }
        }
    }

    private fun validateTimestampPath() {
        if (!_timestampPath.isNullOrEmpty()) {
            ConfigurationException.check(
                (timestampPath != null), "$CONFIG_TIMESTAMP_PATH \"$_timestampPath\" is not a valid JmesPath expression",
                CONFIG_TIMESTAMP_PATH, this)
        }
    }

    private fun validateDataPath() {
        ConfigurationException.check(
            (!_dataPath.isNullOrEmpty()),
            "$CONFIG_DATA_PATH is required",
            CONFIG_DATA_PATH,
            this)

        try {
            JmesPathExtended.create().compile(_dataPath!!)
        } catch (e: Throwable) {
            throw ConfigurationException("$CONFIG_DATA_PATH is not a valid JMESPath expression, $e", CONFIG_DATA_PATH, this)
        }
    }

    companion object {

        private const val CONFIG_NODE_ID = "NodeId"
        private const val CONFIG_DATA_PATH = "DataPath"
        private const val CONFIG_WARN_IF_NOT_PRESENT = "WarnIfNotPresent"
        const val CONFIG_TIMESTAMP_PATH = "TimestampPath"

        const val CONFIG_DATA_TYPE = "DataType"
        const val CONFIG_DIMENSIONS = "Dimensions"

        fun getExpression(path: String?): Expression<Any>? {
            if (path.isNullOrEmpty()) {
                return null
            }

            val p: String = JmesPathExtended.escapeJMesString(path)
            if (!cachedJmespathQueries.containsKey(p)) {
                cachedJmespathQueries[p] = try {
                    jmespath.compile(if (p.startsWith("@.")) p else "@.$p")
                } catch (_: Throwable) {
                    null
                }
            }
            return cachedJmespathQueries[p]
        }

        private val jmespath by lazy {
            JmesPathExtended.create()
        }
        private val cachedJmespathQueries = mutableMapOf<String, Expression<Any>?>()

        val default = OpcuaNodeConfiguration()

        fun create(nodeId: String? = default._nodeId,
                   dataType: String? = default._dataType,
                   dataPath: String? = default._dataPath,
                   warnIfNotPresent: Boolean = default._warnIfNotPresent,
                   timestampPath: String? = default.timestampPathStr): OpcuaNodeConfiguration{

            val instance = OpcuaNodeConfiguration()
            with(instance) {
                _nodeId = nodeId
                _dataType = dataType
                _dataPath = dataPath
                _warnIfNotPresent = warnIfNotPresent
                _timestampPath = timestampPath
            }
            return instance
        }

    }

}