// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config


import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_SOURCES
import com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfiguration.Companion.CONFIG_DEFAULT_WAIT_BEFORE_RETRY
import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcua.config.OpcuaAdapterConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.CONFIG_NODE_ID
import com.amazonaws.sfc.opcua.config.OpcuaNodeChannelConfiguration.Companion.CONFIG_NODE_EVENT_TYPE
import com.amazonaws.sfc.opcua.config.OpcuaServerConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaSourceConfiguration
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode
import java.security.PublicKey
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class OpcuaAutoDiscoveryConfigProvider(
    private val configStr: String,
    private val configVerificationKey: PublicKey?,
    private val logger: Logger
) : ConfigProvider {

    private val className = this::class.java.name.toString()
    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    private var waitBeforeRetry : Duration? =CONFIG_DEFAULT_WAIT_BEFORE_RETRY.toDuration(DurationUnit.MILLISECONDS)

    private var lastConfig: String? = null

    val discoveryWorker = scope.launch {

        val log = logger.getCtxLoggers(className, "discoveryWorker")
        while (true) {

            if (configVerificationKey != null) {
                if (!ConfigVerification.verify(configStr, configVerificationKey)) {
                    log.error("Content of configuration could not be verified")
                    continue
                }
            }

            // get a config reader for the input configuration data
            val configReader = ConfigReader.createConfigReader(configStr, allowUnresolved = true)

            try {

                // get auto discovery config only, this needs verification
                val autoDiscovery: OpcuaAutoDiscoveryConfiguration = configReader.getConfig(true)
                val providerConfig: OpcuaDiscoveryProviderConfig = autoDiscovery.autoDiscoveryProviderConfig
                waitBeforeRetry = autoDiscovery.waitForRetry

                // if no autodiscovery sources just emit received configuration
                if (providerConfig.isEmpty()) {
                    log.warning("No autodiscovery configured")
                    emitConfiguration(configStr)
                    continue
                }

                log.trace("Autodiscovery configured for sources ${providerConfig.keys}")

                // get opcua configuration, validation is switched off as it has not been processed by the config provider
                val opcuaConfigInput: OpcuaConfiguration = configReader.getConfig(validate = false)

                // get the input config as mutable map so  channels for discovered nodes can be added
                @Suppress("UNCHECKED_CAST")
                val configOutput = gsonExtended().fromJson(configStr, Map::class.java) as MutableMap<String, Any>

                @Suppress("UNCHECKED_CAST")
                val configOutputSources = configOutput[CONFIG_SOURCES] as MutableMap<String, Map<String, Any>>

                try {
                    // validate ifd configured sources for autodiscovery are consistent with configured sources
                    checkIfConfiguresAutoDiscoverySourcesExist(opcuaConfigInput, providerConfig, log)
                    checkIfEmptySourcesHaveAutoDiscoveryConfiguration(opcuaConfigInput, providerConfig, log)
                } catch( e)


                // todo test if source has nodes -> if empty remove
                // todo no nodes no returned config

                opcuaConfigInput.sources.forEach { (sourceID, sourceConfig) ->

                    val nodesForSource: List<OpcuaDiscoveryNodeConfiguration> = providerConfig[sourceID] ?: emptyList()

                    if (nodesForSource.isNotEmpty()) {

                        val discoveredNodesForSource =
                            discovererSourceNodesNodes(sourceID, sourceConfig, opcuaConfigInput, nodesForSource)

                        @Suppress("UNCHECKED_CAST")
                        val outputChannelsForSource =
                            configOutputSources[sourceID]?.get(CONFIG_CHANNELS) as MutableMap<String, Map<String, Any>>?

                        discoveredNodesForSource.forEach { discoveredNode: DiscoveredUaNodeType ->
                            val (channelName: String, channelMap) = buildChannelMapEntry(discoveredNode)
                            outputChannelsForSource?.set(channelName, channelMap)
                        }
                    }
                }
                emitConfiguration(buildConfigOutputString(configOutput))
                return@launch

            } catch (e: Exception) {
                log.error("Error executing autodiscovery, $e")
            }

            delay(waitBeforeRetry))
        }

    }

    private fun discovererSourceNodesNodes(
        sourceID: String,
        sourceConfig: OpcuaSourceConfiguration,
        opcuaConfigInput: OpcuaConfiguration,
        nodesForSource: List<OpcuaDiscoveryNodeConfiguration>
    ): List<DiscoveredUaNodeType> {

        val log = logger.getCtxLoggers(className, "discovererSourceNodesNodes")

        val adapterForSource = getSourceProtocolAdapter(sourceID, opcuaConfigInput, sourceConfig, log)
        val server = getServer(sourceID, sourceConfig.sourceAdapterOpcuaServerID, adapterForSource, log)
        val serverProfile = adapterForSource?.serverProfiles?.get(server?.serverProfile)

        val source = OpcuaDiscoverySource(
            sourceID = sourceID,
            configuration = opcuaConfigInput,
            serverProfile = serverProfile,
            logger = logger
        )

        val discoveredNodesForSource = discoverNodes(source, nodesForSource)
        return discoveredNodesForSource
    }

    private fun buildConfigOutputString(configOutput: MutableMap<String, Any>): String {
        val gson = JsonHelper.gsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        return gson.toJson(configOutput)
    }

    private fun discoverNodes(
        source: OpcuaDiscoverySource,
        nodesForSource: List<OpcuaDiscoveryNodeConfiguration>,
    ): List<DiscoveredUaNodeType> {

        val log = logger.getCtxLoggers(className, "discoverNodes")

        return sequence {
            nodesForSource.forEach { node ->
                val nodes =
                    runBlocking {
                        source.discoverNodes(
                            nodeID = node.nodeID,
                            discoveryDepth = node.discoveryDepth,
                            nodeTypesToDiscover = node.nodeTypesToDiscover
                        )
                    }

                val filteredNodes = applyFilters(nodes, node)
                this.yieldAll(filteredNodes)
            }
        }.toList()
    }

    private fun applyFilters(
        nodes: List<DiscoveredUaNodeType>,
        node: OpcuaDiscoveryNodeConfiguration
    ): List<DiscoveredUaNodeType> {
        var filteredNodes = includedNodes(nodes, node)
        filteredNodes = excludeNodes(filteredNodes, node)
        return filteredNodes
    }

    private fun excludeNodes(filteredNodes: List<DiscoveredUaNodeType>, node: OpcuaDiscoveryNodeConfiguration) =
        filteredNodes.filter { n ->
            val isExcludedNode = node.exclusions.any { it.matcher(n.path).matches() }
            if (isExcludedNode) {
                logger.getCtxTraceLog(className, "excludeNodes")
                "Node ${n.path} is excluded by exclusions ${
                    node.exclusions.map {
                        it.pattern().toString()
                    }
                }"
                )
            }
            isExcludedNode
        }

    private fun includedNodes(nodes: List<DiscoveredUaNodeType>, node: OpcuaDiscoveryNodeConfiguration) =
        nodes.filter { n ->
            if (node.inclusions.isEmpty()) true
            else {
                val isIncluded = node.inclusions.any { it.matcher(n.path).matches() }
                logger.getCtxTraceLog(className, "includedNodes")(
                    "Node ${n.path} is included by inclusions ${
                        node.exclusions.map {
                            it.pattern().toString()
                        }
                    }"
                )
                isIncluded
            }
        }


    private fun buildChannelMapEntry(discoveredNode: DiscoveredUaNodeType): Pair<String, MutableMap<String, Any>> {
        val channelName: String = buildIDForNode(discoveredNode.parents, discoveredNode.node)
        val channelMap = mutableMapOf<String, Any>()
        channelMap[CONFIG_NODE_ID] = discoveredNode.node.nodeId.toParseableString()
        if (discoveredNode is DiscoveredEventNode) channelMap[CONFIG_NODE_EVENT_TYPE] =
            discoveredNode.eventType
        return channelName to channelMap
    }

    private suspend fun emitConfiguration(s: String) {
        if (lastConfig != s) {
            ch.send(s)
            lastConfig = s
        }
    }

    private fun getServer(
        sourceID: String,
        sourceAdapterOpcuaServerID: String,
        adapterForSource: OpcuaAdapterConfiguration?,
        log: Logger.ContextLogger,
    ): OpcuaServerConfiguration? {
        return if (adapterForSource != null) {
            val server = adapterForSource.opcuaServers.get(sourceAdapterOpcuaServerID)
            if (server == null) log.error("Server \"${sourceAdapterOpcuaServerID}\" for source \"$sourceID\" does not exists, available servers are ${adapterForSource.opcuaServers}")
            else log.trace("Used server for source \"$sourceID\" is \"$server\"")
            server
        } else null

    }

    private fun getSourceProtocolAdapter(
        sourceID: String,
        opcuaConfigIn: OpcuaConfiguration,
        sourceConfig: OpcuaSourceConfiguration,
        log: Logger.ContextLogger,
    ): OpcuaAdapterConfiguration? {
        val adapterForSource = opcuaConfigIn.protocolAdapters[sourceConfig.protocolAdapterID]
        if (adapterForSource == null) log.error("\"$sourceID\" protocol adapter \"${sourceConfig.protocolAdapterID}\" does not exist, available adapters are ${opcuaConfigIn.protocolAdapters.keys}")
        log.trace("Adapter for source $sourceID is $adapterForSource")
        return adapterForSource
    }

    private fun checkIfEmptySourcesHaveAutoDiscoveryConfiguration(
        opcuaConfigIn: OpcuaConfiguration,
        providerConfig: OpcuaDiscoveryProviderConfig,
        log: Logger.ContextLogger,
    ) {
        opcuaConfigIn.sources.filter { it.value.channels.isEmpty() }.forEach {
            if (!providerConfig.containsKey(it.key))
                log.error("Source \"${it.key}\" has no configured channels and no autodiscovery for this is configured")
        }
    }

    private fun checkIfConfiguresAutoDiscoverySourcesExist(
        opcuaConfigIn: OpcuaConfiguration,
        providerConfig: OpcuaDiscoveryProviderConfig,
        log: Logger.ContextLogger
    ) {
        providerConfig.keys.forEach {
            if (!opcuaConfigIn.sources.keys.contains(it)) {
                log.warning("Autodiscovery is configured for source \"$it\", but this source does not exist")
            }
        }
    }

    private fun buildIDForNode(parents: List<UaNode>, node: UaNode) =
        (parents + listOf(node))
            .joinToString(separator = "/") {
                cleanNameForNode(it)
            }

    private fun cleanNameForNode(node: UaNode): String {
        return node.browseName.name.toString()
            .replace("http://", "")
            .replace("/", "_")
            .replace(":", "_")
            .replace(" ", "")
            .trim('_')
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return OpcuaAutoDiscoveryConfigProvider(
                createParameters[0] as String,
                createParameters[1] as PublicKey?,
                createParameters[2] as Logger
            )
        }

    }

    override val configuration: Channel<String> = ch

}