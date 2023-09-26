/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression
import org.eclipse.milo.opcua.sdk.core.NumericRange
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId

val OpcuaNodeChannelConfiguration.isDataNode
    get() = this.nodeEventType.isNullOrEmpty()

val OpcuaNodeChannelConfiguration.isEventNode
    get() = !this.isDataNode

@ConfigurationClass
class OpcuaNodeChannelConfiguration : ChannelConfiguration() {

    @SerializedName(CONFIG_NODE_ID)
    private var _nodeID: String = ""
    val nodeID: NodeId?
        get() = NodeId.parseOrNull(_nodeID)

    @SerializedName(CONFIG_INDEX_RANGE)
    private var _indexRange: String? = null
    val indexRange: String?
        get() = _indexRange?.trim()

    @SerializedName(CONFIG_NODE_CHANGE_FILTER)
    private var _nodeChangeFilter: OpcuaNodeChangeFilter? = null
    val nodeChangeFilter: OpcuaNodeChangeFilter?
        get() = _nodeChangeFilter

    @SerializedName(CONFIG_NODE_EVENT_TYPE)
    private var _nodeEventType: String? = null
    val nodeEventType: String?
        get() = _nodeEventType

    // Can be used to overwrite source level interval
    @SerializedName(OpcuaSourceConfiguration.CONFIG_EVENT_SAMPLING_INTERVAL)
    private var _nodeEventSamplingInterval: Int? = null
    val eventSamplingInterval: Int?
        get() = _nodeEventSamplingInterval

    @SerializedName(CONFIG_SELECTOR)
    private var _selector: String? = null

    private var _expression: Expression<Any>? = null
    private var error: String? = null
    val selector: Expression<Any>?
        get() {

            return when {

                (_expression != null) -> _expression

                (_selector.isNullOrEmpty()) -> null

                (error != null) -> null

                (_expression == null) -> try {
                    _expression = jmesPath.compile(_selector)
                    _expression
                } catch (e: Throwable) {
                    error = e.message
                    null
                }

                else -> null
            }
        }

    val selectorStr
        get() = _selector

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        validateNodeID()
        validateIndexRange()
        validateSelector()
        nodeChangeFilter?.validate()

        validated = true
    }


    private fun validateNodeID() {
        ConfigurationException.check(_nodeID.isNotEmpty(), "NodeId not specified or empty", "NodeId", this)
        ConfigurationException.check((nodeID != null), "\"$_nodeID\" is not a valid node id", "NodeId", this)
    }


    private fun validateSelector() {
        if (!_selector.isNullOrEmpty()) {
            selector
            ConfigurationException.check(
                (error == null),
                "$CONFIG_SELECTOR \"$_selector\" for node $_nodeID is not a valid JMESPath selector, $error",
                CONFIG_SELECTOR,
                this
            )
        }
    }


    private fun validateIndexRange() {
        if (!indexRange.isNullOrBlank()) {
            try {
                NumericRange.parse(indexRange)
            } catch (e: Throwable) {
                throw ConfigurationException("$CONFIG_INDEX_RANGE' \"$_indexRange\" is not a valid range, ${e.message}", "IndexRange", this)
            }
        }
    }

    override fun toString(): String {
        return "OpcuaNodeChannelConfiguration(nodeID='$_nodeID', indexRange=$_indexRange, nodeChangeFilter=$nodeChangeFilter,selectorStr=$selectorStr)"
    }

    companion object {

        const val CONFIG_INDEX_RANGE = "IndexRange"
        const val CONFIG_NODE_CHANGE_FILTER = "NodeChangeFilter"
        const val CONFIG_SELECTOR = "Selector"
        const val CONFIG_NODE_EVENT_TYPE = "EventType"


        val jmesPath by lazy {
            JmesPathExtended.create()
        }

        private val default = OpcuaNodeChannelConfiguration()

        fun create(nodeId: String = default._nodeID,
                   indexRange: String? = default._indexRange,
                   nodeChangeFilter: OpcuaNodeChangeFilter? = default._nodeChangeFilter,
                   selector: String? = default._selector,
                   nodeEventSamplingInterval: Int? = default._nodeEventSamplingInterval,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID,
                   nodeEventType: String? = default._nodeEventType): OpcuaNodeChannelConfiguration {

            val instance = createChannelConfiguration<OpcuaNodeChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter)

            with(instance) {
                _nodeID = nodeId
                _indexRange = indexRange
                _nodeChangeFilter = nodeChangeFilter
                _selector = selector
                _nodeEventSamplingInterval = nodeEventSamplingInterval
                _nodeEventType = nodeEventType

            }
            return instance
        }
    }

}