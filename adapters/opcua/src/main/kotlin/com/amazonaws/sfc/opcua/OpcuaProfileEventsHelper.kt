/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

import com.amazonaws.sfc.opcua.FilterHelper.Companion.DEFAULT_EVENT_TYPE
import com.amazonaws.sfc.opcua.config.OpcuaEventTypeConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaServerProfileConfiguration
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant

class OpcuaProfileEventsHelper(private val serverProfile: OpcuaServerProfileConfiguration) : EventsHelper {

    class ProfileEventData(override val name: String,
                           override val identifier: NodeId,
                           override val eventProperties: List<QualifiedName>,
                           override val properties: List<Pair<NodeId, QualifiedName>>) : Event


    private val opcuaEventsHelper = OpcuaEventsHelper()
    private val serverProfileEvents = serverProfile.eventTypes

    override fun findEvent(name: String?): Event? {
        if (name == null) return null
        var event = allProfileEvents[name] as Event?
        if (event != null) return event

        val nodeId = NodeId.parseOrNull(name)
        event = if (nodeId != null) findEvent(nodeId) else null
        return event ?: opcuaEventsHelper.findEvent(name)
    }

    override fun findEvent(identifier: NodeId?): Event? = allProfileEvents.values.firstOrNull {
        it.identifier == identifier
    } ?: opcuaEventsHelper.findEvent(identifier)


    override fun isKnownEvent(name: String) =
        if (findEvent(name) != null) true else {
            opcuaEventsHelper.isKnownEvent(name)
        }

    override val allEventClassNames by lazy { serverProfile.eventTypes.entries.map { it.key } + opcuaEventsHelper.allEventClassNames }
    override val allEventClassIdentifiers by lazy { serverProfile.eventTypes.entries.map { it.value.nodeID } + opcuaEventsHelper.allEventClassIdentifiers }

    override val allEventClasses by lazy { allEventClassNames.zip(allEventClassIdentifiers) }

    private val allProfileEvents = sequence {
        serverProfile.eventTypes.forEach { (name, eventTypeConfiguration) ->
            val eventTypeNodeId = eventTypeConfiguration.nodeID
            if (eventTypeNodeId != null) {
                val properties = getEventProperties(name, serverProfile.eventTypes)
                this.yield(name to ProfileEventData(name,
                    eventTypeNodeId,
                    eventTypeConfiguration.properties,
                    properties))
            }
        }
    }.toMap()

    override fun variantPropertiesToMap(eventVariantValues: Array<Variant>,
                                        eventProperties: List<Pair<NodeId, QualifiedName>>,
                                        context: SerializationContext): Map<String, Any> =
        opcuaEventsHelper.variantPropertiesToMap(eventVariantValues, eventProperties, context)


    private fun getEventProperties(name: String,
                                   profileEvents: Map<String, OpcuaEventTypeConfiguration>,
                                   trail: MutableList<String> = mutableListOf()): List<Pair<NodeId, QualifiedName>> {

        // prevent circular inheritance
        if (name in trail) return emptyList()

        // lookup event name and if not found by the node id
        val event = getProfileEventConfiguration(name, trail)

        if (event?.nodeID == null) return getEventProperties(DEFAULT_EVENT_TYPE, profileEvents, trail)

        val properties = event.properties.map { event.nodeID!! to it }.toMutableList()

        if (event.inheritsFrom != null) {
            // opcua event class
            val opcuaEvent = opcuaEventsHelper.findEvent(event.inheritsFrom)
            if (opcuaEvent != null) {
                properties.addAll(opcuaEvent.properties)
            } else {
                // inherits from server profile event
                properties.addAll(getEventProperties(event.inheritsFrom!!, profileEvents, trail))
            }
        }

        return properties
    }


    private fun getProfileEventConfiguration(
        name: String,
        trail: MutableList<String>): OpcuaEventTypeConfiguration? {

        var event = serverProfileEvents[name]

        if (event != null) {
            trail.addAll(listOf(name, event.nodeID.toString()))
        } else {
            val nodeID = NodeId.parseOrNull(name)
            val entry = serverProfileEvents.entries.firstOrNull {
                it.value.nodeID == nodeID
            }
            if (entry != null) {
                trail.addAll(listOf(entry.key, nodeID.toString()))
                event = entry.value
            }
        }
        return event
    }

    companion object {
        fun createHelper(serverProfile: OpcuaServerProfileConfiguration?) = if (serverProfile == null) OpcuaEventsHelper() else OpcuaProfileEventsHelper(serverProfile)
    }

}
