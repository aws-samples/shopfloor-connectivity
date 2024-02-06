// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.AutoDiscoveryProviderConfiguration.Companion.CONFIG_DEFAULT_MAX_RETRIES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_SOURCES
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
import java.util.regex.Pattern
import kotlin.time.Duration


// Custom config provider using autodiscovery to dynamically add channels to OPCUA source nodes from node
class OpcuaAutoDiscoveryConfigProvider(
    private val configStr: String,
    private val configVerificationKey: PublicKey?,
    private val logger: Logger
) : ConfigProvider {

    private val className = this::class.java.simpleName
    private val scope = buildScope("CustomConfigProvider")

    private var waitBeforeRetry: Duration? = null
    private var maxRetries = CONFIG_DEFAULT_MAX_RETRIES
    private var retryCount = 0

    // channel to pass configuration to SFC core
    override val configuration: Channel<String> = Channel<String>(1)

    private var lastConfig: String? = null

    private var providerConfig: AutoDiscoveryProviderConfiguration? = AutoDiscoveryProviderConfiguration()

    val discoveryWorker = scope.launch {

        val log = logger.getCtxLoggers(className, "discoveryWorker")

        var allSourcesSuccessfullyDiscovered = false

        while (!allSourcesSuccessfullyDiscovered) {

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
                providerConfig = autoDiscovery.autoDiscoveryProviderConfig

                // if no autodiscovery sources just emit received configuration
                if (providerConfig == null) {
                    log.error("No autodiscovery configured")
                    return@launch
                }

                // make config setting available in class scope
                waitBeforeRetry = providerConfig?.waitForRetry
                maxRetries = providerConfig?.maxRetries ?: CONFIG_DEFAULT_MAX_RETRIES

                log.trace("Autodiscovery configured for sources ${providerConfig!!.sources.keys}")

                // get opcua configuration, validation is switched off as it has not been processed by the config provider
                val opcuaConfigInput: OpcuaConfiguration = configReader.getConfig(validate = false)

                // get the input config as mutable map so  channels for discovered nodes can be added
                @Suppress("UNCHECKED_CAST")
                val configOutput = gsonExtended().fromJson(configStr, Map::class.java) as MutableMap<String, Any>

                // the sources in the output configuration
                @Suppress("UNCHECKED_CAST")
                val configOutputSources = configOutput[CONFIG_SOURCES] as MutableMap<String, Map<String, Any>>

                try {
                    // validate if configured sources for autodiscovery are consistent with configured sources
                    checkIfConfiguredAutoDiscoverySourcesExist(opcuaConfigInput, providerConfig)
                    checkIfEmptySourcesHaveAutoDiscoveryConfiguration(opcuaConfigInput, providerConfig)
                } catch (e: Exception) {
                    log.error("Error validating autodiscovery configuration, $e")
                    return@launch
                }

                allSourcesSuccessfullyDiscovered = true

                // process all configured sources
                opcuaConfigInput.sources.forEach { (sourceID, sourceConfig) ->

                    // add channels for discovered node to the source configuration
                    //   addNodeChannelsForSource(sourceID, sourceConfig, opcuaConfigInput, configOutputSources)

                    // If no nodes were discovered then run autodiscovery after a wait period (if waitBeforeRetry is configured)
                    if (addNodeChannelsForSource(sourceID, sourceConfig, opcuaConfigInput, configOutputSources) == 0) {
                        log.error("No nodes discovered for source $sourceID")
                        allSourcesSuccessfullyDiscovered = false
                    }
                }

                // emit configuration if created sources configuration is valid
                if (validateSources(configOutputSources, log)) {
                    emitConfiguration(buildConfigOutputString(configOutput))
                }

                // all sources have been discovered successfully, no need for retry to failed sources
                if (allSourcesSuccessfullyDiscovered) {
                    return@launch
                }

            } catch (e: Exception) {
                log.error("Error executing autodiscovery, $e")
            }

            // If a period for retry was configured retry if discovery for a source did not return any nodes.
            retryCount += 1
            if (waitBeforeRetry == null || retryCount > maxRetries) return@launch

            log.error("Retrying autodiscovery in $waitBeforeRetry")
            delay(waitBeforeRetry!!)
        }

    }

    private fun addNodeChannelsForSource(
        sourceID: String,
        sourceConfig: OpcuaSourceConfiguration,
        opcuaConfigInput: OpcuaConfiguration,
        configOutputSources: MutableMap<String, Map<String, Any>>
    ): Int {

        // nodes for source to browse
        val nodesForSource: NodeDiscoveryConfigurations = providerConfig?.sources?.get(sourceID) ?: emptyList()

        if (nodesForSource.isEmpty()) return 0

        // discover nodes for this source
        val discoveredNodesForSource = discoverSourceNodesNodes(sourceID, sourceConfig, opcuaConfigInput, nodesForSource)

        // channels configuration for source
        @Suppress("UNCHECKED_CAST")
        var outputChannelsForSource = configOutputSources[sourceID]?.get(CONFIG_CHANNELS) as MutableMap<String, Map<String, Any>>?
        if (outputChannelsForSource == null) {
            outputChannelsForSource = mutableMapOf()
            (configOutputSources[sourceID] as MutableMap<String, Any>?)?.set(CONFIG_CHANNELS, outputChannelsForSource)
        }

        // Add a channel for every discovered node
        discoveredNodesForSource.forEach { discoveredNode: DiscoveredNode ->
            val (channelName: String, channelMap) = buildNodeChannelMapEntry(discoveredNode)
            outputChannelsForSource[channelName] = channelMap
        }

        // return number of added channels
        return discoveredNodesForSource.size
    }

    private fun validateSources(
        configOutputSources: MutableMap<String, Map<String, Any>>, log: Logger.ContextLogger
    ): Boolean {

        // filter out all sources without any channels
        val sourcesWithChannels = configOutputSources.filter { (s, c) ->
            @Suppress("UNCHECKED_CAST")
            val channelsForSource = c[CONFIG_CHANNELS] as MutableMap<String, Map<String, Any>>?
            if (channelsForSource.isNullOrEmpty()) {
                log.error("Removing source \"$s\" from configuration as it has no channels after auto discovery")
                false
            } else true
        }

        // configuration is valid if it does not contain any sources
        if (sourcesWithChannels.isEmpty()) {
            log.error("No sources with channels after auto discovery")
            return false
        }
        return true
    }

    private fun discoverSourceNodesNodes(
        sourceID: String,
        sourceConfig: OpcuaSourceConfiguration,
        opcuaConfigInput: OpcuaConfiguration,
        nodesForSource: NodeDiscoveryConfigurations
    ): DiscoveredNodes {

        // get the adapter used for the source
        val adapterForSource = getProtocolAdapterForSource(sourceID, opcuaConfigInput, sourceConfig)

        // get the server used to read this source
        val server = if (adapterForSource != null) getServerForSource(sourceID, sourceConfig.sourceAdapterOpcuaServerID, adapterForSource) else null

        // get the server profile, as it is needed to validate the event type of discovered event nodes
        val serverProfile = adapterForSource?.serverProfiles?.get(server?.serverProfile)

        // create an instance of OPCUA source used to discover the nodes
        val opcuaSource = OpcuaDiscoverySource(
            sourceID = sourceID,
            configuration = opcuaConfigInput,
            serverProfile = serverProfile,
            logger = logger
        )

        // discover and return nodes for this source
        return discoverNodes(opcuaSource, nodesForSource)
    }


    private fun discoverNodes(source: OpcuaDiscoverySource, nodesForSource: NodeDiscoveryConfigurations): DiscoveredNodes {

        val log = logger.getCtxLoggers(className, "discoverNodes")

        // get sub nodes up to specified depth
        val nodes = sequence {
            nodesForSource.forEach { node ->
                val nodes = runBlocking {
                    source.discoverNodes(
                        nodeID = node.nodeID,
                        discoveryDepth = node.discoveryDepth,
                        nodeTypesToDiscover = node.nodeTypesToDiscover
                    )
                }

                // apply node filtering
                val filteredNodes = applyNodeFilters(nodes, node)
                this.yieldAll(filteredNodes)
            }
        }.toList()

        log.info("Discovered ${nodes.size} nodes for source ${source.sourceID}")

        return nodes
    }

    private fun applyNodeFilters(nodesToFilter: DiscoveredNodes, nodeConfiguration: NodeDiscoveryConfiguration): DiscoveredNodes {
        val filteredNodes = selectIncludedNodes(nodesToFilter, nodeConfiguration.inclusions)
        return filterOutExcludedNodes(filteredNodes, nodeConfiguration.exclusions)
    }

    private fun filterOutExcludedNodes(nodes: DiscoveredNodes, exclusionPatterns: List<Pattern>): DiscoveredNodes {
        val trace = logger.getCtxTraceLog(className, "excludeNodes")
        return nodes.filter { node ->
            if (exclusionPatterns.isEmpty()) true
            else {
                var excluded = false
                exclusionPatterns.forEach { excludePattern ->
                    if (!excluded) {
                        // exclude if pathname of node matches this exclude pattern
                        excluded = excludePattern.matcher(node.path).matches()
                        if (excluded) trace("Node \"${node.path}\" is excluded by pattern \"${excludePattern.pattern()}\"")
                    }
                }
                !excluded
            }
        }
    }

    private fun selectIncludedNodes(nodes: DiscoveredNodes, inclusionPatterns: List<Pattern>): DiscoveredNodes {

        val trace = logger.getCtxTraceLog(className, "includedNodes")

        return nodes.filter { node ->
            if (inclusionPatterns.isEmpty()) true
            else {
                var included = false
                inclusionPatterns.forEach { includePattern ->
                    if (!included) {
                        // include if pathname of node matches this include pattern
                        included = includePattern.matcher(node.path).matches()
                        if (included) trace("Node \"${node.path}\" is included by pattern \"${includePattern.pattern()}\"")
                    }
                }
                included
            }
        }
    }


    private fun buildNodeChannelMapEntry(discoveredNode: DiscoveredNode): Pair<String, MutableMap<String, Any>> {

        // build the channelID to use as key in the map
        val channelID: String = buildChannelIDForNode(discoveredNode.parents, discoveredNode.node)

        val newChannel = mutableMapOf<String, Any>()

        // the node to read/register for the channel
        newChannel[CONFIG_NODE_ID] = discoveredNode.node.nodeId.toParseableString()

        // if the node is an event add the type of the event
        if (providerConfig?.includeDescription == true) {
            val description = discoveredNode.node.description?.text
            if (!description.isNullOrEmpty()) {
                newChannel[CONFIG_DESCRIPTION] = discoveredNode.node.description.text.toString()
            }
        }

        // if the node has a description add it as description for the channel
        if (discoveredNode is EventNode) newChannel[CONFIG_NODE_EVENT_TYPE] =
            discoveredNode.eventType

        return channelID to newChannel
    }


    private suspend fun emitConfiguration(configStr: String) {
        if (lastConfig != configStr) {
            val log = logger.getCtxLoggers(className, "emitConfiguration")
            log.trace("Emitting configuration to SFC core\n$configStr")
            // send the configuration to the SFC Core
            configuration.send(configStr)
            saveConfiguration(configStr)
            lastConfig = configStr
        }
    }

    private fun saveConfiguration(configStr: String) {

        // if a file name is configured then save the configuration to that file
        val savedLastConfig = providerConfig?.savedLastConfig
        if (savedLastConfig != null) {
            val log = logger.getCtxLoggers(className, "saveConfiguration")
            try {
                log.info("Saving config to $savedLastConfig")
                savedLastConfig.writeText(configStr)
            } catch (e: Exception) {
                log.error("Error saving last configuration to file $savedLastConfig, $e")
            }
        }
    }


    private fun getServerForSource(
        sourceID: String,
        sourceAdapterOpcuaServerID: String,
        sourceAdapter: OpcuaAdapterConfiguration
    ): OpcuaServerConfiguration? {

        val log = logger.getCtxLoggers(className, "getServerForSource")
        val server = sourceAdapter.opcuaServers[sourceAdapterOpcuaServerID]
        if (server == null) log.error("Server \"${sourceAdapterOpcuaServerID}\" for source \"$sourceID\" does not exists, available servers are ${sourceAdapter.opcuaServers}")
        else log.trace("Used server for source \"$sourceID\" is \"$server\"")

        return server
    }

    private fun getProtocolAdapterForSource(
        sourceID: String,
        inputConfiguration: OpcuaConfiguration,
        sourceConfig: OpcuaSourceConfiguration
    ): OpcuaAdapterConfiguration? {
        val log = logger.getCtxLoggers(className, "getProtocolAdapterForSource")
        val adapterForSource = inputConfiguration.protocolAdapters[sourceConfig.protocolAdapterID]
        if (adapterForSource == null) log.error("\"$sourceID\" protocol adapter \"${sourceConfig.protocolAdapterID}\" does not exist, available adapters are ${inputConfiguration.protocolAdapters.keys}")
        log.trace("Adapter for source $sourceID is $adapterForSource")
        return adapterForSource
    }

    private fun checkIfEmptySourcesHaveAutoDiscoveryConfiguration(opcuaConfigIn: OpcuaConfiguration, providerConfig: AutoDiscoveryProviderConfiguration?) {
        val log = logger.getCtxLoggers(className, "checkIfEmptySourcesHaveAutoDiscoveryConfiguration")
        if (providerConfig == null) return
        opcuaConfigIn.sources.filter { it.value.channels.isEmpty() }.forEach {
            if (!providerConfig.sources.containsKey(it.key))
                log.error("Source \"${it.key}\" has no configured channels and no autodiscovery for this is configured")
        }
    }

    private fun checkIfConfiguredAutoDiscoverySourcesExist(inputConfiguration: OpcuaConfiguration, providerConfig: AutoDiscoveryProviderConfiguration?) {
        val log = logger.getCtxLoggers(className, "checkIfConfiguredAutoDiscoverySourcesExist")
        providerConfig?.sources?.keys?.forEach {
            if (!inputConfiguration.sources.keys.contains(it)) {
                log.warning("Autodiscovery is configured for source \"$it\", but this source does not exist")
            }
        }
    }


    companion object {

        private fun buildConfigOutputString(configOutput: MutableMap<String, Any>): String {
            val gson = JsonHelper.gsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            return gson.toJson(configOutput)
        }

        private fun buildChannelIDForNode(parents: List<UaNode>, node: UaNode) =
            // the channel is a concatenation, with a '/' separator of cleaned up browse names of the path for the node
            (parents + listOf(node))
                .joinToString(separator = "/") {
                    cleanupNameForNode(it)
                }


        private fun cleanupNameForNode(node: UaNode): String {
            // cleanup browse name for use as channelID for the generated channels
            return node.browseName.name.toString()
                .replace("http://", "http_")
                .replace("/", "_")
                .replace(":", "_")
                .replace(" ", "")
                .trim('_')
        }

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

}