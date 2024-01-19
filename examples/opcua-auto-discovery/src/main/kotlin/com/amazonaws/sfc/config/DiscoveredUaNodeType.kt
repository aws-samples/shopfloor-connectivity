package com.amazonaws.sfc.config

import org.eclipse.milo.opcua.sdk.client.nodes.UaNode

sealed class DiscoveredUaNodeType(val node: UaNode, val parents: List<UaNode>){
    val path:String by lazy { (parents.map { it.browseName.name }.plus(node.browseName.name)).joinToString(separator = "/") }
}
class DiscoveredVariableNode(node: UaNode, parents: List<UaNode>) : DiscoveredUaNodeType(node, parents)
class DiscoveredEventNode(node: UaNode, parents: List<UaNode>, val eventType: String) : DiscoveredUaNodeType(node, parents)