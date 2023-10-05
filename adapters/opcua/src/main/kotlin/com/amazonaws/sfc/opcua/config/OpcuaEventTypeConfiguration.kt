/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_INHERITS_FROM
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_PROPERTIES
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName


@ConfigurationClass
class OpcuaEventTypeConfiguration : Validate, OpcuaEvent {

    @SerializedName(CONFIG_NODE_ID)
    private var _nodeID: String = ""
    override val nodeID: NodeId?
        get() = NodeId.parseOrNull(_nodeID)

    @SerializedName(CONFIG_PROPERTIES)
    private var _properties: List<String> = emptyList()
    override val properties: List<QualifiedName>
        get() = _properties.map { QualifiedName.parse(it) }

    @SerializedName(CONFIG_INHERITS_FROM)
    private var _inheritsFrom: String? = null
    val inheritsFrom: String?
        get() = _inheritsFrom

    private var _validated = false

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {

        ConfigurationException.check(
            _nodeID.isNotEmpty(),
            "$CONFIG_NODE_ID of event type can not be empty",
            CONFIG_NODE_ID,
            this
        )

        ConfigurationException.check(
            NodeId.parseOrNull(_nodeID) != null,
            "$CONFIG_NODE_ID \"$nodeID\" invalid node syntax",
            CONFIG_NODE_ID,
            this
        )

        ConfigurationException.check(
            _properties.isNotEmpty(),
            "$CONFIG_PROPERTIES of event type must contain one or more properties",
            CONFIG_PROPERTIES,
            this
        )

        ConfigurationException.check(
            _properties.all { it.isNotBlank() },
            "Event property can not be an empty string",
            CONFIG_PROPERTIES,
            this
        )
    }

    companion object {

        private val default = OpcuaEventTypeConfiguration()

        fun create(nodeId: String = default._nodeID,
                   properties: List<String> = default._properties): OpcuaEventTypeConfiguration {

            val instance = OpcuaEventTypeConfiguration()

            with(instance) {
                _nodeID = nodeId
                _properties = properties

            }
            return instance
        }
    }

}