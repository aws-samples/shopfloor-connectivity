// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.AutoDiscoveryProviderConfiguration.Companion.CONFIG_DISCOVERY_DEPTH
import com.amazonaws.sfc.config.AutoDiscoveryProviderConfiguration.Companion.CONFIG_EXCLUSIONS
import com.amazonaws.sfc.config.AutoDiscoveryProviderConfiguration.Companion.CONFIG_INCLUSIONS
import com.amazonaws.sfc.config.AutoDiscoveryProviderConfiguration.Companion.CONFIG_NODES_TO_DISCOVER
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import java.util.regex.Pattern

typealias NodeDiscoveryConfigurations = List<NodeDiscoveryConfiguration>

class NodeDiscoveryConfiguration : Validate {

    @SerializedName(CONFIG_NODE_ID)
    private var _nodeID: String = ""

    val nodeID: NodeId
        get() = NodeId.parse(_nodeID)

    @SerializedName(CONFIG_INCLUSIONS)
    private var _inclusions: List<String> = emptyList()
    // list of regex pattern, nodes are included if this list is empty or if it contains a pattern matching the node path name
    val inclusions: List<Pattern> by lazy {
        _inclusions.map { Pattern.compile(it) }
    }

    @SerializedName(CONFIG_EXCLUSIONS)
    private var _exclusions: List<String> = emptyList()
    // list of regex pattern, nodes are excluded if this list contains a pattern matching the node path name
    val exclusions: List<Pattern> by lazy {
        _exclusions.map { Pattern.compile(it) }
    }

    @SerializedName(CONFIG_NODES_TO_DISCOVER)
    private var _nodeTypesToDiscover: DiscoveredNodeTypes = DiscoveredNodeTypes.Variables
    // select variable nodes, known event/alarm types or both
    val nodeTypesToDiscover: DiscoveredNodeTypes
        get() = _nodeTypesToDiscover

    // level of sub nodes browsed for discovery, 0 means no limit
    @SerializedName(CONFIG_DISCOVERY_DEPTH)
    private var _discoveryDepth: Int = 0
    val discoveryDepth
        get() = _discoveryDepth

    override fun validate() {

        if (validated) return

        validateNodeID()
        validateInclusions()
        validateExclusions()

        validated = true
    }

    private fun validateExclusions() {
        try {
            _exclusions.forEach { r -> Pattern.compile(r) }
        } catch (
            e: Exception
        ) {
            throw ConfigurationException(
                "$CONFIG_EXCLUSIONS invalid regex expression in $_exclusions, $e",
                CONFIG_EXCLUSIONS,
                this
            )
        }
    }

    private fun validateInclusions() {
        try {
            _inclusions.forEach { r -> Pattern.compile(r) }
        } catch (
            e: Exception
        ) {
            throw ConfigurationException(
                "$CONFIG_INCLUSIONS invalid regex expression in $_inclusions, $e",
                CONFIG_INCLUSIONS,
                this
            )
        }
    }

    private fun validateNodeID() {
        ConfigurationException.check(
            _nodeID.trim().isNotEmpty(),
            "$CONFIG_NODE_ID must be set",
            CONFIG_NODE_ID,
            this
        )

        try {
            NodeId.parse(_nodeID)
        } catch (
            e: Exception
        ) {
            throw ConfigurationException(
                "$CONFIG_NODE_ID \"$_nodeID\" is not a valid node id, $e",
                CONFIG_NODE_ID,
                this
            )
        }
    }

    private var _validated = false

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }
}