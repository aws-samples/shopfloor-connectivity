// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcua.OpcuaProfileEventsHelper
import com.amazonaws.sfc.opcua.OpcuaSource
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaServerProfileConfiguration
import org.eclipse.milo.opcua.sdk.client.AddressSpace
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId
import java.util.concurrent.atomic.AtomicInteger

// THis class extends the OPCUA Adapter source class with the functionality to browse nodes
class OpcuaDiscoverySource(
    val sourceID: String,
    configuration: OpcuaConfiguration,
    serverProfile: OpcuaServerProfileConfiguration?,
    private val logger: Logger
) : OpcuaSource(sourceID, configuration, AtomicInteger(), logger, null, emptyMap()) {

    private val className = this::class.java.simpleName

    // helper to get all OPCUA and configured companion spec event/alarm types
    private val eventsHelper by lazy {
        OpcuaProfileEventsHelper.createHelper(serverProfile)
    }

    // discovers child nodes for a given node
    suspend fun discoverNodes(
        nodeID: NodeId,
        discoveryDepth: Int,
        nodeTypesToDiscover: DiscoveredNodeTypes
    ): DiscoveredNodes {

        val log = logger.getCtxLoggers(className, "discoverNodes")

        // get a client for this source using the configuration for the source
        val opcuaClient = this.getClient()

        if (opcuaClient == null) {
            log.error("Could not create client to discover nodes from source \"$sourceID\" from node ${nodeID.toParseableString()}")
            return emptyList()
        }

        return try {
            browseNodes(
                opcuaClient,
                nodeID,
                depth = 0,
                nodeTypesToDiscover = nodeTypesToDiscover,
                maxDepth = discoveryDepth
            )
        } finally {
            //  opcuaClient.disconnect()
        }

    }

    // browses sub nodes of a given node and return these as a list of nodes
    private fun browseNodes(
        client: OpcUaClient,
        nodeID: NodeId,
        path: List<UaNode> = emptyList(),
        nodeTypesToDiscover: DiscoveredNodeTypes,
        depth: Int = 0,
        maxDepth: Int = 0
    ): DiscoveredNodes {

        val log = logger.getCtxLoggers(className, "browseNodes")

        if (maxDepth != 0 && depth >= maxDepth) {
            log.trace("Max depth reached, returning")
            return emptyList()
        }

        // sets flags for what node types  must be returned
        val shouldDiscoverVariables =
            nodeTypesToDiscover == DiscoveredNodeTypes.Variables || nodeTypesToDiscover == DiscoveredNodeTypes.VariablesAndEvents
        val shouldDiscoverEvents =
            nodeTypesToDiscover == DiscoveredNodeTypes.Events || nodeTypesToDiscover == DiscoveredNodeTypes.VariablesAndEvents

        val nodes = try {
            client.addressSpace?.browseNodes(
                nodeID,
                AddressSpace.BrowseOptions.builder()
                    .setNodeClassMask(setOf(NodeClass.Object, NodeClass.Variable))
                    .build()
            )
        } catch (e: Exception) {
            log.errorEx("Error  browsing nodes for node ${nodeID.toParseableString()}", e)
            return emptyList()
        }

        if (nodes?.size != 0) log.trace("Read ${nodes?.size} nodes for ${nodeID.toParseableString()}")

        return sequence {
            nodes?.forEach { node: UaNode ->

                if (shouldDiscoverVariables && node.nodeClass == NodeClass.Variable) {
                    // variable node
                    log.trace("Found variable node ${node.nodeId}, ${node.displayName.text}")
                    yield(VariableNode(node, path))

                } else {
                    // event/alarm node
                    if (shouldDiscoverEvents && node.nodeClass == NodeClass.Object) {

                        // get the event type of the node, this can be a OPCUA standard node or configured type
                        // configured in the profile of the OPCUA server, see OPCUA adapter and SFC documentation or details
                        val eventTypeName = getEventType(client, node)
                        if (eventTypeName != null) {
                            log.trace("Found event node ${node.nodeId} of type $eventTypeName, ${node.displayName.text}")

                            yield(EventNode(node, path, eventTypeName))

                        } else {
                            if (path.contains(node)) {
                                log.warning("Circular reference for node ${node.nodeId.toParseableString()} in path ${path.joinToString(separator = "/") { it.nodeId.toParseableString() }}, skipping")
                            } else {
                                // browse sub nodes
                                yieldAll(
                                    browseNodes(
                                        client = client,
                                        nodeID = node.nodeId,
                                        path = path + listOf(node),
                                        nodeTypesToDiscover = nodeTypesToDiscover,
                                        depth = depth + 1,
                                        maxDepth = maxDepth
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }.toList()
    }

    // THis method reads an event node and returns the event type, this can be a OPCUA standard node or configured type
    private fun getEventType(client: OpcUaClient, node: UaNode): String? {

        val log = logger.getCtxLoggers(className, "getEventType")

        // get sub nodes for an event
        val subNodes = try {
            client.addressSpace?.browseNodes(
                node.nodeId,
                AddressSpace.BrowseOptions.builder().setNodeClassMask(setOf(NodeClass.Variable)).build()
            )
        } catch (e: Exception) {
            log.errorEx("Error reading sub nodes for node ${node.nodeId}", e)
            return null
        }

        // find the node that holds the type of the event
        val eventTypeNode = subNodes?.find { it.browseName.name == "EventType" } ?: return null

        // read the event type for event node
        val readValue = ReadValueId(eventTypeNode.nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
        return try {
            val readResponse = client.read(0.0, TimestampsToReturn.Both, mutableListOf(readValue)).get()
            val eventTypeNodeID: NodeId? = readResponse.results.first().value.value as NodeId?
            log.trace("Read event type for node ${node.nodeId} from sub node ${eventTypeNode.nodeId}, $eventTypeNodeID")

            // Check id this is a known event type, either an OPCUA type event or a configured event defined in the
            // profile used to read from the  server
            val i = eventsHelper.allEventClassIdentifiers.indexOf(eventTypeNodeID)
            if (i == -1) {
                log.error("Event ${eventTypeNode.nodeId} for node ${node.nodeId} is not a known event type")
                null
            } else {
                eventsHelper.allEventClassNames[i]
            }
        } catch (e: Exception) {
            log.errorEx("Error reading event type for node ${node.nodeId} from sub node ${eventTypeNode.nodeId}", e)
            null
        }
    }
}

