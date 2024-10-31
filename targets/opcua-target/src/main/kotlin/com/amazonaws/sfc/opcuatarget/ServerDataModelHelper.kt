// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuatarget.config.DataModelConfiguration
import com.amazonaws.sfc.opcuatarget.config.FolderNodeConfiguration
import com.amazonaws.sfc.opcuatarget.config.OpcuaTargetConfiguration
import com.amazonaws.sfc.opcuatarget.config.VariableNodeConfiguration
import com.amazonaws.sfc.transformations.Transformation
import io.burt.jmespath.Expression
import org.eclipse.milo.opcua.sdk.server.OpcUaServer
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import java.time.Instant
import java.util.*

class ServerDataModelHelper(private val server: OpcUaServer,
                            private val targetConfiguration: OpcuaTargetConfiguration,
                            private val elementNames: ElementNamesConfiguration,
                            private val transformations: Map<String, Transformation>,
                            private val attributeFilter: AttributeFilter?,
                            private val logger: Logger) {

    class CachedQuery(val id: String,
                      val uaVariableNode: UaVariableNode,
                      val queryStr: String,
                      val query: Expression<Any>?,
                      val transformationID: String?)

    inner class Folder(val folderConfig: FolderNodeConfiguration, val folderNode: UaFolderNode)


    private val className = this::class.java.simpleName

    private val cachedFolders = mutableMapOf<String, UaFolderNode?>()
    private val cachedVariables = mutableMapOf<String, UaVariableNode?>()

    private val cacheValueQueries = mutableMapOf<NodeId, CachedQuery>()
    private val cachedTimestampQueries = mutableMapOf<NodeId, Pair<String?, Expression<Any>?>>()

    private val namespaceBuilders: MutableMap<Int, OpcuaNamespaceBuilder> = mutableMapOf()
    private val variableNodes = mutableListOf<Pair<String, UaVariableNode>>()
    private val folders = mutableListOf<Folder>()
    private val namespaceFolders = mutableListOf<UaFolderNode>()

    val valueQueries: Map<NodeId, CachedQuery>
        get() = cacheValueQueries.filter { it.value.query != null }

    val timestampQueries: Map<NodeId, Pair<String?, Expression<Any>?>>
        get() = cachedTimestampQueries.filter { it.value.second != null }

    private fun storeVariable(id: String, uaVariableNode: UaVariableNode) {
        variableNodes.add(id to uaVariableNode)
    }

    private fun storeNamespaceFolder(modelConfig: DataModelConfiguration, uaFolderNode: UaFolderNode) {
        namespaceFolders.add(uaFolderNode)
        storeFolder(modelConfig, uaFolderNode)
    }

    private fun storeFolder(folderNodeConfig: FolderNodeConfiguration, uaFolderNode: UaFolderNode) {
        val folder = Folder(folderNodeConfig, uaFolderNode)
        folders.add(folder)

    }

    private fun buildKey(scheduleName: String = "", sourceName: String = "", channelName: String = "", valueName: String = "", metadataName: String = ""): String {
        return "${scheduleName}\\${sourceName}\\${channelName}\\$valueName\\${metadataName}"
    }

    private fun buildNodeIdStr(vararg s: String) = s.filter { it.isNotEmpty() }.joinToString("/") { it.capitalized() }

    private fun variableById(id: String): UaVariableNode? =
        variableNodes.firstOrNull { it.first == id }?.second

    private fun folderNodeById(id: String): Folder? =
        folders.firstOrNull { it.folderConfig.id == id }

    private fun folderByBrowseName(browseName: QualifiedName?): Folder? =
        if (browseName == null) null else folders.firstOrNull { it.folderNode.browseName == browseName }


    private fun folderByNodeId(nodeId: NodeId): Folder? = folders.firstOrNull { it.folderConfig.nodeID == nodeId }

    private fun getOrCreateFolder(id: String,
                                  folderId: String,
                                  parent: UaFolderNode?,
                                  key: String,
                                  displayName: String,
                                  fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        if (parent == null) return null

        if (cachedFolders.containsKey(key)) return cachedFolders[key]

        val existingFolder = searchFolder(parent, folderId)
        if (existingFolder != null) {
            cachedFolders[key] = existingFolder
            return existingFolder
        }

        if (targetConfiguration.autoCreate) {
            val newFolderConfiguration = FolderNodeConfiguration(
                id = id,
                displayName = displayName,
                namespaceIndex = parent.nodeId.namespaceIndex.toInt())

            val folderNode = createFolder(parent.nodeId, newFolderConfiguration)
            cachedFolders[key] = folderNode

            if (folderNode != null) fnModelUpdated?.invoke(folderNode)

            return folderNode
        }

        return null

    }

    fun getScheduleFolder(scheduleName: String, fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {


        val folder = getOrCreateFolder(
            id = buildNodeIdStr(scheduleName),
            folderId = scheduleName,
            parent = namespaceFolders.first(),
            displayName = scheduleName.capitalized(),
            key = buildKey(scheduleName = scheduleName),
            fnModelUpdated = fnModelUpdated)

        return folder
    }


    fun getSourceFolder(scheduleName: String, sourceName: String, fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        val folder = getOrCreateFolder(
            id = buildNodeIdStr(scheduleName, sourceName),
            folderId = sourceName,
            parent = getScheduleFolder(scheduleName, fnModelUpdated),
            displayName = sourceName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName),
            fnModelUpdated = fnModelUpdated)
        return folder
    }

    fun getChannelFolder(scheduleName: String, sourceName: String, channelName: String, fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        val sourceFolder = getSourceFolder(scheduleName, sourceName, fnModelUpdated)
        val folder = getOrCreateFolder(
            id = buildNodeIdStr(scheduleName, sourceName, channelName),
            folderId = channelName,
            displayName = channelName,
            parent = sourceFolder,
            fnModelUpdated = fnModelUpdated,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName))

        return folder
    }


    fun getChannelVariable(folder: UaFolderNode?,
                           scheduleName: String,
                           sourceName: String,
                           channelName: String,
                           valueName: String = "",
                           value: Any,
                           fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        if (folder == null) return null
        val idStr = buildNodeIdStr(scheduleName, sourceName, channelName, valueName)
        val variableNode = getOrCreateVariable(
            id = idStr,
            name = channelName,
            folder = folder,
            displayName = (valueName.ifEmpty { channelName }).capitalized(),
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName, valueName = valueName),
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }


    private fun searchVariable(folder: UaFolderNode, id: String): UaVariableNode? {

        val log = logger.getCtxLoggers(className, "searchVariable")


        val folderConfiguration = folderByBrowseName(folder.browseName)?.folderConfig
        if (folderConfiguration != null) {
            log.trace("Search variable with ID \"$id\" by ID \"$id\"")
            val variableByIdInFolder = folderConfiguration.variables?.get(id)
            if (variableByIdInFolder != null) {
                val variableByID = variableById(id)
                if (variableByID != null) {
                    log.trace("Found variable with ID \"$id\", variable is ${variableByID.nodeId.toParseableString()}}")
                    return variableByID
                } else {
                    log.trace("No variable with ID \"$id\" found")
                }
            }
        }

        return null

    }

    private fun searchFolder(folder: UaFolderNode, id: String): UaFolderNode? {

        val log = logger.getCtxLoggers(className, "searchFolder")
        log.trace("Find folder with ID \"$id\" within folder ${folder.nodeId.toParseableString()}")

        val folderConfiguration = folderByNodeId(folder.nodeId)?.folderConfig
        if (folderConfiguration != null) {
            log.trace("Search folder with ID \"$id\" by ID \"$id\"")
            val folderByID = folderNodeById(id)
            if (folderByID != null) {
                log.trace("Found folder with ID \"$id\", folder is ${folderByID.folderNode.nodeId.toParseableString()}}")
                return folderByID.folderNode
            } else {
                log.trace("No folder with ID \"$id\" found")
            }
        }
        return null
    }

    private fun getOrCreateVariable(id: String,
                                    name: String,
                                    folder: UaFolderNode,
                                    key: String,
                                    displayName: String,
                                    value: Any,
                                    fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        if (cachedVariables.containsKey(key)) return cachedVariables[key]


        val existingNode = searchVariable(folder, name)

        if (existingNode != null) {
            cachedVariables[key] = existingNode
            return existingNode
        }

        if (targetConfiguration.autoCreate) {

            val newVariableNodeConfiguration = VariableNodeConfiguration(
                id = id,
                namespaceIndex = folder.nodeId.namespaceIndex.toInt(),
                displayName = displayName,
                dataType = dataTypeForValue(value),
                arrayDimensions = dimensionsForValue(value)
            )

            val variableNode = createVariable(folder, newVariableNodeConfiguration)
            cachedVariables[key] = variableNode

            if (variableNode != null) fnModelUpdated?.invoke(variableNode)

            return variableNode
        }

        return null

    }

    private fun createVariable(folder: UaFolderNode, nodeConfig: VariableNodeConfiguration): UaVariableNode? {


        nodeConfig.nameSpaceIndex = folder.nodeId.namespaceIndex.toInt()
        val namespaceBuilder = namespaceBuilders[folder.nodeId.namespaceIndex.toInt()]
        val variableNode = namespaceBuilder?.addVariableNode(nodeConfig, folder)
        return variableNode

    }

    private fun createFolder(parent: NodeId, nodeConfig: FolderNodeConfiguration): UaFolderNode? {

        nodeConfig.nameSpaceIndex = parent.namespaceIndex.toInt()
        val namespaceBuilder = namespaceBuilders[parent.namespaceIndex.toInt()]
        val folderNode = namespaceBuilder?.addFolderNode(nodeConfig, parent.expanded())
        return folderNode

    }


    fun createServerDataModels() {

        val log = logger.getCtxLoggers(className, "createModels")
        targetConfiguration.dataModels.values.forEach {

            val ns = OpcuaNamespaceBuilder(server, it, Identifiers.ObjectsFolder.expanded(), attributeFilter, targetConfiguration.valuesInitWithNull, logger)
            log.info("Creating namespace ${ns.namespaceIndex}:${it.id} for model ${it.id}")

            ns.onNamespaceFolderNodeCreated = { modelConfig, uaFolderNode ->
                storeNamespaceFolder(modelConfig, uaFolderNode)
            }

            ns.onVariableNodeCreated = { variableNodeConfig, uaVariableNode ->
                val selectorStr = variableNodeConfig.valueSelector

                if (selectorStr != null && !cacheValueQueries.containsKey(uaVariableNode.nodeId)) {
                    try {

                        if (!variableNodeConfig.transformationID.isNullOrEmpty() && !transformations.containsKey(variableNodeConfig.transformationID)) {

                            log.error("Unable to find transformation \"${variableNodeConfig.transformationID}\" for variable ${variableNodeConfig.id}, configured transformations are ${transformations.keys}")
                            cacheValueQueries[uaVariableNode.nodeId] = CachedQuery(
                                id = variableNodeConfig.id,
                                uaVariableNode = uaVariableNode,
                                queryStr = selectorStr,
                                query = null,
                                transformationID = variableNodeConfig.transformationID)
                        }

                        if (!cacheValueQueries.containsKey(uaVariableNode.nodeId)){
                            cacheValueQueries[uaVariableNode.nodeId] = CachedQuery(
                                id = variableNodeConfig.nodeID?.toParseableString() ?: variableNodeConfig.browseName,
                                uaVariableNode = uaVariableNode,
                                queryStr = selectorStr,
                                query = JmesPathExtended.create().compile(JmesPathExtended.escapeJMesString(selectorStr)),
                                transformationID = variableNodeConfig.transformationID)
                        }

                        val timestampSelector = getValueTimestampSelector(variableNodeConfig.timestampSelector, variableNodeConfig.valueSelector!!, elementNames)
                        cachedTimestampQueries[uaVariableNode.nodeId] = timestampSelector

                    } catch (e: Exception) {
                        log.error("Unable to compile selector $selectorStr for variable ${variableNodeConfig.id}, $e")
                        cacheValueQueries[uaVariableNode.nodeId] = CachedQuery(
                            id = variableNodeConfig.id,
                            uaVariableNode = uaVariableNode,
                            queryStr = selectorStr,
                            query = null,
                            transformationID = null)
                    }

                }
                storeVariable(variableNodeConfig.id, uaVariableNode)

            }

            ns.onFolderNodeCreated = { folderNodeConfig, uaFolderNode ->
                storeFolder(folderNodeConfig, uaFolderNode)
            }

            namespaceBuilders[ns.namespaceIndex.toInt()] = ns

            ns.startup()
        }
    }

    private fun getValueTimestampSelector(timestampSelectorStr: String?, valueSelectorStr: String, elementNames: ElementNamesConfiguration): Pair<String?, Expression<Any>?> {

        val log = logger.getCtxLoggers(className, "getValueTimestamp")

        if (timestampSelectorStr != null) {
            return try {
                log.trace("Using query \"$timestampSelectorStr\" to get timestamp")
                timestampSelectorStr to JmesPathExtended.create().compile(JmesPathExtended.escapeJMesString(timestampSelectorStr))
            } catch (e: Exception) {
                log.errorEx("Error compiling timestamp selector \"$timestampSelectorStr\"", e)
                timestampSelectorStr to null
            }
        }

        val valueTimeStampPathStr = if (valueSelectorStr.endsWith(elementNames.value)) {
            val pathElements = valueSelectorStr.split(".").toMutableList()
            pathElements[pathElements.lastIndex] = elementNames.timestamp
            pathElements.joinToString(separator = ".")
        } else null

        if (valueTimeStampPathStr != null) {
            return try {
                val q = valueTimeStampPathStr to JmesPathExtended.create().compile(JmesPathExtended.escapeJMesString(valueTimeStampPathStr))
                log.trace("Using implicit query \"$valueTimeStampPathStr\" to get timestamp")
                q
            } catch (e: Exception) {
                valueTimeStampPathStr to null
            }
        }

        return null to null
    }


    fun getScheduleMetadataVariable(scheduleFolder: UaFolderNode, scheduleName: String, metadataName: String, value: Any, fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, metadataName),
            name = metadataName,
            folder = scheduleFolder,
            displayName = metadataName,
            key = buildKey(scheduleName = scheduleName, metadataName = metadataName),
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }

    fun getSourceMetadataVariable(sourceFolder: UaFolderNode, scheduleName: String, sourceName: String, metadataName: String, value: Any, fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {
        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, sourceName, metadataName),
            name = metadataName,
            folder = sourceFolder,
            displayName = metadataName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, metadataName = metadataName),
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }

    fun getChannelMetadataVariable(sourceFolder: UaFolderNode,
                                   scheduleName: String,
                                   sourceName: String,
                                   channelName: String,
                                   metadataName: String,
                                   value: Any,
                                   fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {
        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, sourceName, channelName, metadataName),
            name = metadataName,
            folder = sourceFolder,
            displayName = metadataName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName, metadataName = metadataName),
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }


    fun getChannelAggregatedValueVariable(folder: UaFolderNode?,
                                          scheduleName: String,
                                          sourceName: String,
                                          channelName: String,
                                          aggregationName: String,
                                          value: Any,
                                          fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        if (folder == null) return null

        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, sourceName, channelName, aggregationName),
            name = aggregationName,
            folder = folder,
            displayName = aggregationName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName, metadataName = aggregationName),
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }

    companion object {
        private fun dimensionsForValue(value: Any): List<Int> {
            return if (value is List<*>) {
                val dimensions = mutableListOf(value.size)
                if (value.first() != null && value.first() is List<*>) {
                    dimensions += dimensionsForValue(value.first() as List<*>)
                }
                dimensions
            } else emptyList()

        }

        fun dataTypeForValue(value: Any): OpcuaServerDataTypes {

            val v: Any? = if (value is List<*>) if (value.isEmpty() || value.first() == null) null else (value.first() as Any) else value
            if (v == null) return OpcuaServerDataTypes.VARIANT

            return when (v) {
                is Boolean -> OpcuaServerDataTypes.BOOLEAN
                is Byte -> OpcuaServerDataTypes.BYTE
                is UByte -> OpcuaServerDataTypes.UBYTE
                is Short -> OpcuaServerDataTypes.SHORT
                is UShort -> OpcuaServerDataTypes.USHORT
                is Int -> OpcuaServerDataTypes.INT
                is UInt -> OpcuaServerDataTypes.UINT
                is Long -> OpcuaServerDataTypes.LONG
                is ULong -> OpcuaServerDataTypes.ULONG
                is Float -> OpcuaServerDataTypes.FLOAT
                is Double -> OpcuaServerDataTypes.DOUBLE
                is Char -> OpcuaServerDataTypes.STRING
                is String -> OpcuaServerDataTypes.STRING
                is Instant -> OpcuaServerDataTypes.DATETIME
                is Map<*, *> -> OpcuaServerDataTypes.STRUCT
                is ChannelOutputData -> if (v.value != null) dataTypeForValue(v.value!!) else OpcuaServerDataTypes.VARIANT
                else -> OpcuaServerDataTypes.VARIANT
            }
        }

        fun String.capitalized(): String {
            return this.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else it.toString()
            }
        }

    }

}