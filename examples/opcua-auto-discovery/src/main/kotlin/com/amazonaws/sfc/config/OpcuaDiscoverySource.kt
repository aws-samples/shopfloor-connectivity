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


class OpcuaDiscoverySource(
    private val sourceID: String,
    configuration: OpcuaConfiguration,
    serverProfile: OpcuaServerProfileConfiguration?,
    private val logger: Logger
) : OpcuaSource(sourceID, configuration, AtomicInteger(), logger, null, emptyMap()) {

    private val className = this::class.java.name.toString()


    // helper to get all OPCUA and configured companion spec event/alarm types
    private val eventsHelper by lazy {
        OpcuaProfileEventsHelper.createHelper(serverProfile)
    }

    // discovers child nodes for a given node
    suspend fun discoverNodes(
        nodeID: NodeId,
        discoveryDepth: Int,
        nodeTypesToDiscover: DiscoveryNodeTypes
    ): List<DiscoveredUaNodeType> {

        val log = logger.getCtxLoggers(className, "discoverNodes")

        // get a client for this source using the configuration for the source
        val opcuaClient = this.getClient()

        if (opcuaClient == null) {
            log.error("Could not create client to discover nodes from source \"$sourceID\"")
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
            opcuaClient.disconnect()
        }

    }

    // browses sub nodes of a given node and return these as a sequence of nodes
    private fun browseNodes(
        client: OpcUaClient,
        nodeID: NodeId,
        parents: List<UaNode> = emptyList(),
        nodeTypesToDiscover: DiscoveryNodeTypes,
        depth: Int = 0,
        maxDepth: Int = 0
    ): List<DiscoveredUaNodeType> {

        val log = logger.getCtxLoggers(className, "browseNodes")

        if (maxDepth != 0 && depth >= maxDepth) {
            log.trace("Max depth reached, returning")
            return emptyList()
        }

        val shouldDiscoverVariables =
            nodeTypesToDiscover == DiscoveryNodeTypes.Variables || nodeTypesToDiscover == DiscoveryNodeTypes.VariablesAndEvents
        val shouldDiscoverEvents =
            nodeTypesToDiscover == DiscoveryNodeTypes.Events || nodeTypesToDiscover == DiscoveryNodeTypes.VariablesAndEvents

        val nodes = try {
            client.addressSpace?.browseNodes(
                nodeID,
                AddressSpace.BrowseOptions.builder()
                    .setNodeClassMask(setOf(NodeClass.Object, NodeClass.Variable))
                    .build()
            )
        } catch (e: Exception) {
            log.error("Error  browsing nodes for node ${nodeID}, $e ")
            return emptyList()
        }

        log.trace("Read ${nodes?.size} nodes for $nodeID")


        return sequence {
            nodes?.forEach { node: UaNode ->

                if (shouldDiscoverVariables && node.nodeClass == NodeClass.Variable) {
                    log.trace("Found variable node ${node.nodeId}, ${node.displayName.text}")
                    yield(DiscoveredVariableNode(node, parents))
                } else {
                    if (shouldDiscoverEvents && node.nodeClass == NodeClass.Object) {
                        val eventTypeName = getEventType(client, node)
                        if (eventTypeName != null) {
                            log.trace("Found event node ${node.nodeId} of type $eventTypeName, ${node.displayName.text}")
                            yield(DiscoveredEventNode(node, parents, eventTypeName))
                        } else {
                            yieldAll(
                                browseNodes(
                                    client = client,
                                    nodeID = node.nodeId,
                                    parents = parents + listOf(node),
                                    nodeTypesToDiscover = nodeTypesToDiscover,
                                    depth = depth + 1,
                                    maxDepth = maxDepth
                                )
                            )
                        }
                    }
                }
            }
        }.toList()
    }


    private fun getEventType(client: OpcUaClient, node: UaNode): String? {

        val log = logger.getCtxLoggers(className, "getEventType")

        val subNodes = try {
            client.addressSpace?.browseNodes(
                node.nodeId,
                AddressSpace.BrowseOptions.builder().setNodeClassMask(setOf(NodeClass.Variable)).build()
            )
        } catch (e: Exception) {
            log.error("Error reading sub nodes for node ${node.nodeId} ")
            return null
        }

        val eventType = subNodes?.find { it.browseName.name == "EventType" } ?: return null

        val readValue = ReadValueId(eventType.nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
        return try {
            val readResponse = client.read(0.0, TimestampsToReturn.Both, mutableListOf(readValue)).get()
            val eventTypeNodeID: NodeId? = readResponse.results.first().value.value as NodeId?
            log.trace("Read event type for node ${node.nodeId} from sub node ${eventType.nodeId}, $eventTypeNodeID")
            val i = eventsHelper.allEventClassIdentifiers.indexOf(eventTypeNodeID)
            if (i == -1) {
                log.error("Event ${eventType.nodeId} for node ${node.nodeId} is not a known event type")
                null
            } else {
                eventsHelper.allEventClassNames[i]
            }
        } catch (e: Exception) {
            log.error("Error reading event type for node ${node.nodeId} from sub node ${eventType.nodeId}, $e ")
            null
        }
    }
}

