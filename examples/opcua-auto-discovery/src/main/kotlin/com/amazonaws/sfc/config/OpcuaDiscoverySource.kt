// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcua.OpcuaProfileEventsHelper
import com.amazonaws.sfc.opcua.OpcuaSource
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaServerConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaServerProfileConfiguration
import com.amazonaws.sfc.service.RateLimiter
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.delay
import org.eclipse.milo.opcua.sdk.client.AddressSpace
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId
import java.util.concurrent.atomic.AtomicInteger

// This class extends the OPCUA Adapter source class with the functionality to browse nodes
class OpcuaDiscoverySource(
    val sourceID: String,
    configuration: OpcuaConfiguration,
    private val serverConfig: OpcuaServerConfiguration?,
    serverProfile: OpcuaServerProfileConfiguration?,
    private val rateLimit: Int = 0,
    private val logger: Logger
) : OpcuaSource(sourceID, configuration, AtomicInteger(), logger, null, emptyMap()) {

    private val className = this::class.java.simpleName

    private val rateLimiter = if (rateLimit != 0) RateLimiter(rateLimit) else null

    private suspend fun acquireClientRead() {
        if (rateLimit <= 0) return
        val start = systemDateTime().toEpochMilli()
        while (true) {
            if (rateLimiter?.tryAcquire() == true) {
                return
            }
            if (systemDateTime().toEpochMilli() - start > 1000) {
                throw Exception("Rate limit of $rateLimit browse and read calls per seconds exceeded for over 1 second")
            }
            delay(1000 /rateLimit.toLong())
        }
    }

    // helper to get all OPCUA and configured companion spec event/alarm types
    private val eventsHelper by lazy {
        OpcuaProfileEventsHelper.createHelper(serverProfile)
    }

    // discovers child nodes for a given node
    suspend fun discoverNodes(nodeID: NodeId, discoveryDepth: Int, nodeTypesToDiscover: DiscoveredNodeTypes, prefix: String?): DiscoveredNodes {

        val log = logger.getCtxLoggers(className, "discoverNodes")

        // get a client for this source using the configuration for the source
        val opcuaClient = this.getClient()

        if (opcuaClient == null) {
            log.error("Could not create client to discover nodes from source \"$sourceID\" from node ${nodeID.toParseableString()}")
            return emptyList()
        }

        return try {

            acquireClientRead()

            browseNodes(
                client = opcuaClient,
                nodeID = nodeID,
                depth = 0,
                prefix = prefix,
                nodeTypesToDiscover = nodeTypesToDiscover,
                maxDepth = discoveryDepth
            )
        } catch (e: Exception) {
            log.error("Error browsing node ${nodeID.toParseableString()}, $e")
            emptyList()
        }

    }

    // browses sub nodes of a given node and return these as a list of nodes
    private suspend fun browseNodes(
        client: OpcUaClient,
        nodeID: NodeId,
        path: List<UaNode> = emptyList(),
        prefix: String?,
        nodeTypesToDiscover: DiscoveredNodeTypes,
        depth: Int = 0,
        maxDepth: Int = 0
    ): DiscoveredNodes {

        val log = logger.getCtxLoggers(className, "browseNodes")

        if (maxDepth != 0 && depth >= maxDepth) {
            log.trace("Max depth of $maxDepth reached, returning")
            return emptyList()
        }

        // sets flags for what node types  must be returned
        val shouldDiscoverVariables =
            nodeTypesToDiscover == DiscoveredNodeTypes.Variables || nodeTypesToDiscover == DiscoveredNodeTypes.VariablesAndEvents
        val shouldDiscoverEvents =
            nodeTypesToDiscover == DiscoveredNodeTypes.Events || nodeTypesToDiscover == DiscoveredNodeTypes.VariablesAndEvents

        val mask = setOf<NodeClass>(NodeClass.Object,NodeClass.Variable)
        val browseOptions = AddressSpace.BrowseOptions.builder()
            .setNodeClassMask(mask)
            .build()

        val nodes = try {
            acquireClientRead()
            client.addressSpace?.browseNodes(nodeID, browseOptions)
        } catch (e: Exception) {
            log.error("Error browsing node ${nodeID.toParseableString()}, $e")
            return emptyList()
        }

        if (nodes?.size != 0) log.trace("Read ${nodes?.size} nodes for ${nodeID.toParseableString()}")


        val discoveredNodes = mutableListOf<DiscoveredNode>()

        if (shouldDiscoverVariables) {
            nodes?.filter { node -> node.isVariable }?.forEach { node ->
                if (!node.readable){
                    log.warning("Node ${node.browseName.name} (${node.nodeId.toParseableString()}) is not readable, skipping")
                } else {
                    log.trace("Found variable node ${node.nodeId}, ${node.displayName.text}")
                    discoveredNodes.add(VariableNode(node, path, prefix))
                }
            }
        }

        val noVariableNodes = nodes?.filter { node -> node.nodeClass != NodeClass.Variable }

        val eventNodesWithTypes: Map<UaNode, String> = getEventNodesWithTypes(client, noVariableNodes!!)
        if (shouldDiscoverEvents) {
            eventNodesWithTypes.forEach { (node, type) ->
                log.trace("Found event node ${node.nodeId}, ${node.displayName.text} of type $type")
                discoveredNodes.add(EventNode(node, path, type, prefix))
            }
        }

        val subLevelNodes = noVariableNodes.filter { it !in eventNodesWithTypes.keys }
        subLevelNodes.forEach { node ->
            val discoveredSubLevelNodes = browseNodes(
                client = client,
                nodeID = node.nodeId,
                path = path + listOf(node),
                nodeTypesToDiscover = nodeTypesToDiscover,
                depth = depth + 1,
                prefix = prefix,
                maxDepth = maxDepth
            )
            discoveredNodes.addAll(discoveredSubLevelNodes)
        }

        return discoveredNodes
    }

    private suspend fun getEventNodesWithTypes(client: OpcUaClient, nodes: List<UaNode>): Map<UaNode, String> {

        val log = logger.getCtxLoggers(className, "getEventTypes")

        val eventNodes: Map<UaNode, UaNode> = nodes.mapNotNull { node ->

            val browseOptions = AddressSpace.BrowseOptions.builder()
                .setNodeClassMask(setOf(NodeClass.Variable))
                .build()

            val subNodes = client.addressSpace?.browseNodes(node.nodeId, browseOptions)

            val eventTypeNode = subNodes?.find { it.isEventTypeNode }
            if (eventTypeNode == null) {
                null
            } else {
                node to eventTypeNode
            }
        }.toMap()

        val readBatchSize: Int = serverConfig?.readBatchSize ?: 100

        val eventsWithTypes: MutableMap<UaNode, String> = mutableMapOf()

        eventNodes.entries.chunked(readBatchSize).forEachIndexed { chunkIndex: Int, chunk: List<Map.Entry<UaNode, UaNode>> ->

            val chunkReadIDs: List<ReadValueId> = chunk.map { c: Map.Entry<UaNode, UaNode> ->
                ReadValueId(c.value.nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
            }

            try {

                acquireClientRead()

                val readResponse = client.read(0.0, TimestampsToReturn.Neither, chunkReadIDs).get()

                readResponse.results.forEachIndexed { resultIndex: Int, result ->
                    val eventTypeNodeID: NodeId? = result.value.value as NodeId?
                    val eventNameIndex = eventsHelper.allEventClassIdentifiers.indexOf(eventTypeNodeID)
                    if (eventNameIndex == -1) {
                        log.error("Event ${chunk[resultIndex]} for node ${nodes[resultIndex + chunkIndex * readBatchSize]} is not a known event type")
                    } else {
                        val eventTypeName = eventsHelper.allEventClassNames[eventNameIndex]
                        eventsWithTypes[nodes[resultIndex + chunkIndex * readBatchSize]] = eventTypeName
                    }
                }
            } catch (e: Exception) {
                log.error("Error reading event types for ${chunk.map { it.key.nodeId.toParseableString() }}, $e")
            }
        }

        return eventsWithTypes

    }
    companion object
    {
        val UaNode.readable : Boolean
            get() {
                return ((this.readAttribute(AttributeId.UserAccessLevel).value.value as UByte).toInt() and 0x01) == 0x01
            }

        val UaNode.isVariable : Boolean
            get() {
                return this.nodeClass == NodeClass.Variable
            }

        val UaNode.isEventTypeNode : Boolean
            get(){
                return this.browseName.name == "EventType"
            }
    }
}

