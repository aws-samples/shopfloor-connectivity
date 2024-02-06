// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import org.eclipse.milo.opcua.sdk.client.nodes.UaNode

// Base lass to hold data for discovered nodes
sealed class DiscoveredNode(val node: UaNode, val parents: List<UaNode>) {
    val path: String by lazy { (parents.map { it.browseName.name }.plus(node.browseName.name)).joinToString(separator = "/") }
}

// Discovered variable node
class VariableNode(node: UaNode, parents: List<UaNode>) : DiscoveredNode(node, parents)


// Discovered event node
class EventNode(node: UaNode, parents: List<UaNode>, val eventType: String) : DiscoveredNode(node, parents)

typealias DiscoveredNodes = List<DiscoveredNode>