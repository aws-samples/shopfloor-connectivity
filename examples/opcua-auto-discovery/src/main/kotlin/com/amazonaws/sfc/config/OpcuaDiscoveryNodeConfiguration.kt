package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfiguration.Companion.CONFIG_DISCOVERY_DEPTH
import com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfiguration.Companion.CONFIG_EXCLUSIONS
import com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfiguration.Companion.CONFIG_NODES_TO_DISCOVER
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import java.util.regex.Pattern

class OpcuaDiscoveryNodeConfiguration : Validate {

    @SerializedName(CONFIG_NODE_ID)
    private var _nodeID: String = ""

    val nodeID: NodeId
        get() = NodeId.parse(_nodeID)

    @SerializedName(CONFIG_EXCLUSIONS)
    private var _exclusions: List<String> = emptyList()

    val exclusions: List<Pattern> by lazy{
        _exclusions.map { Pattern.compile(it)}
    }

    @SerializedName(CONFIG_NODES_TO_DISCOVER)
    private var _nodeTypesToDiscover: DiscoveryNodeTypes = DiscoveryNodeTypes.Variables

    val nodeTypesToDiscover: DiscoveryNodeTypes
        get() = _nodeTypesToDiscover

    @SerializedName(CONFIG_DISCOVERY_DEPTH)
    private var _discoveryDepth: Int = 0

    val discoveryDepth
        get() = _discoveryDepth

    override fun validate() {

        if (validated) return

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

        try {
            _exclusions.forEach {  r -> Pattern.compile(r)}
        } catch (
            e: Exception
        ) {
            throw ConfigurationException(
                "$CONFIG_EXCLUSIONS invalid regex expression in $_exclusions, $e",
                CONFIG_EXCLUSIONS,
                this
            )
        }



        validated = true
    }

    private var _validated = false

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }
}