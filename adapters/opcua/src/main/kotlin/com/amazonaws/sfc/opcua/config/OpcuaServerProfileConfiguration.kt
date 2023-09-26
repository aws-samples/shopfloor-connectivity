/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazonaws.sfc.opcua.OpcuaProfileEventsHelper
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_INHERITS_FROM
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId

@ConfigurationClass
class OpcuaServerProfileConfiguration : Validate {

    @SerializedName(CONFIG_SERVER_EVENT_TYPES)
    private var _eventTypes = emptyMap<String, OpcuaEventTypeConfiguration>()
    val eventTypes: Map<String, OpcuaEventTypeConfiguration>
        get() = _eventTypes

    private var _validated = false

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    private fun loopCheck(name: String, event: OpcuaEventTypeConfiguration, trail: MutableList<String> = mutableListOf()) {
        if (event.inheritsFrom.isNullOrBlank()) return

        val nodeID = NodeId.parseOrNull(event.inheritsFrom!!)
        ConfigurationException.check(
            !trail.contains(event.inheritsFrom) && (nodeID == null || !trail.contains(nodeID.toString())),
            "Event type inheritance loop detected for $CONFIG_INHERITS_FROM \"${event.inheritsFrom}\", trail is $trail",
            CONFIG_INHERITS_FROM,
            event
        )

        trail.add(name)
        trail.add(event.nodeID.toString())

        val next = this.eventTypes.entries.firstOrNull { it.key == event.inheritsFrom || it.value.nodeID == nodeID }
        if (next != null) {
            loopCheck(next.key, next.value, trail)
        }
    }

    override fun validate() {
        if (validated) return

        _eventTypes.values.forEach { it.validate() }

        validateInheritanceTypes()

        eventTypes.forEach {
            loopCheck(it.key, it.value)
        }

        validated = true
    }

    private fun validateInheritanceTypes() {

        if (_eventTypes.values.any { !it.inheritsFrom.isNullOrEmpty() }) {
            val eventHelper = OpcuaProfileEventsHelper(this)

            _eventTypes.filter { !it.value.inheritsFrom.isNullOrBlank() }.forEach {


                ConfigurationException.check(
                    (it.value.inheritsFrom != it.key),
                    "$CONFIG_INHERITS_FROM \"${it.value.inheritsFrom}\" can not be the same as its event name or it's event name",
                    CONFIG_INHERITS_FROM,
                    it.value
                )

                val nodeID = NodeId.parseOrNull(it.value.inheritsFrom!!)
                ConfigurationException.check(
                    (nodeID == null) || (nodeID != it.value.nodeID),
                    "$CONFIG_INHERITS_FROM \"${it.value.inheritsFrom}\" can not be the node id as its event name or it's $CONFIG_NODE_ID",
                    CONFIG_INHERITS_FROM,
                    it.value
                )

                ConfigurationException.check(
                    eventHelper.isKnownEvent(it.value.inheritsFrom!!),
                    "$CONFIG_INHERITS_FROM \"${it.value}\" for event type \"${it.key}\" not a valid event type, " +
                    "valid event types are ${eventHelper.allEventClasses.sortedBy { e -> e.toString() }}",
                    OpcuaNodeChannelConfiguration.CONFIG_NODE_EVENT_TYPE,
                    it.value)


            }
        }
    }

    companion object {
        const val CONFIG_SERVER_EVENT_TYPES = "EventTypes"

        private val default = OpcuaServerProfileConfiguration()

        fun create(eventTypes: Map<String, OpcuaEventTypeConfiguration> = default._eventTypes): OpcuaServerProfileConfiguration {

            val instance = OpcuaServerProfileConfiguration()

            with(instance) {
                _eventTypes = eventTypes

            }
            return instance
        }
    }

}