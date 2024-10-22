// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuatarget.config.*
import com.amazonaws.sfc.opcuatarget.config.DataModelMappingBase.Companion.CONFIG_MAPPING_ID
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

class ServerDataModelHelper(private val server: OpcUaServer, private val config: OpcuaTargetConfiguration, private val attributeFilter: AttributeFilter?, private val logger: Logger) {

    private inner class Model(val modelConfig: DataModelConfiguration, val folderNode: UaFolderNode)
    private inner class Folder(val folderConfig: FolderNodeConfiguration, val folderNode: UaFolderNode)

    private inner class ScheduleConfiguration(val mapping: ScheduleMapping?, val model: Model?)
    private inner class SourceConfiguration(val mapping: SourceMapping?, val folder: Folder?)
    private inner class ChannelConfiguration(val mapping: DataModelMappingBase?)

    private val className = this::class.java.simpleName

    private val cachedFolders = mutableMapOf<String, UaFolderNode?>()
    private val cachedVariables = mutableMapOf<String, UaVariableNode?>()

    private val cachedScheduleConfigurations = mutableMapOf<String, ScheduleConfiguration?>()
    private val cachedSourceConfigurations = mutableMapOf<Pair<String, String>, SourceConfiguration?>()
    private val cachedChannelConfigurations = mutableMapOf<Triple<String, String, String>, ChannelConfiguration?>()

    private val namespaceBuilders: MutableMap<Int, OpcuaNamespaceBuilder> = mutableMapOf()
    private val variableNodes = mutableListOf<Pair<String, UaVariableNode>>()
    private val folders = mutableListOf<Folder>()
    private val namespaceFolders = mutableListOf<Model>()

    private fun storeVariable(id: String, uaVariableNode: UaVariableNode) {
        variableNodes.add(id to uaVariableNode)
    }

    private fun storeNamespaceFolder(modelConfig: DataModelConfiguration, uaFolderNode: UaFolderNode) {
        val model = Model(modelConfig, uaFolderNode)

        namespaceFolders.add(model)
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


    private fun namespaceFolderById(id: String) = namespaceFolders.firstOrNull { it.modelConfig.id == id }

    private fun variableByBrowseName(browseName: QualifiedName): UaVariableNode? =
        variableNodes.firstOrNull { it.second.browseName == browseName }?.second

    private fun variableById(id: String): UaVariableNode? =
        variableNodes.firstOrNull { it.first == id }?.second

    private fun folderNodeById(id: String): Folder? =
        folders.firstOrNull { it.folderConfig.id == id }

    private fun folderByBrowseName(browseName: QualifiedName?): Folder? =
        if (browseName == null) null else folders.firstOrNull { it.folderNode.browseName == browseName }

    private fun folderByNodeId(nodeId: NodeId): Folder? = folders.firstOrNull { it.folderConfig.nodeID == nodeId }


    private fun getScheduleConfiguration(schedule: String): ScheduleConfiguration? {

        if (cachedScheduleConfigurations.containsKey(schedule)) return cachedScheduleConfigurations[schedule]

        val scheduleMapping = config.datamodelMapping[schedule]

        val mappingID = scheduleMapping?.mappingID
        if (scheduleMapping != null) {
            if (!mappingID.isNullOrEmpty()) {
                val scheduleByID = namespaceFolderById(mappingID)
                val scheduleConfig = if (scheduleByID != null) ScheduleConfiguration(scheduleMapping, Model(scheduleByID.modelConfig, scheduleByID.folderNode)) else null
                cachedScheduleConfigurations[schedule] = scheduleConfig
                return scheduleConfig
            }
        }

        config.dataModels.forEach { (_: String, model: DataModelConfiguration) ->
            val f: FolderNodeConfiguration? = model.findFolder(schedule)
            if (f?.nodeID != null) {
                val nodeID = QualifiedName(f.nodeID!!.namespaceIndex, f.nodeID!!.identifier.toString())
                val folderNode = folderByBrowseName(nodeID)
                if (folderNode != null) {
                    val scheduleConfig = ScheduleConfiguration(scheduleMapping, Model(model, folderNode.folderNode))
                    cachedScheduleConfigurations[schedule] = scheduleConfig
                    return scheduleConfig
                }
            }
        }

        val scheduleByID = config.dataModels[schedule]
        if (scheduleByID != null) {
            val scheduleFolder = folderNodeById(schedule)?.folderNode
            if (scheduleFolder != null) {
                val scheduleConfig = ScheduleConfiguration(scheduleMapping, Model(scheduleByID, scheduleFolder))
                cachedScheduleConfigurations[schedule] = scheduleConfig
                return scheduleConfig
            }
        }

        namespaceBuilders.forEach { (namespaceIndex, namespaceBuilder) ->

            val browseName = QualifiedName((namespaceIndex), schedule)

            val folderByBrowseName = folderByBrowseName(browseName)

             if ((folderByBrowseName?.folderNode != null)) {
                val model = Model(DataModelConfiguration(folderByBrowseName.folderConfig, namespaceBuilder.namespaceUri), folderByBrowseName.folderNode)
                model.modelConfig.nameSpaceIndex = namespaceBuilder.namespaceIndex.toInt()
                val scheduleConfiguration = ScheduleConfiguration(scheduleMapping, model)
                cachedScheduleConfigurations[schedule] = scheduleConfiguration
                return scheduleConfiguration
            }
        }

        val scheduleConfiguration = ScheduleConfiguration(scheduleMapping, null)
        cachedScheduleConfigurations[schedule] = scheduleConfiguration
        return scheduleConfiguration
    }


    private fun getSourceConfiguration(schedule: String, source: String): SourceConfiguration {

        val keyPair = schedule to source

        if (cachedSourceConfigurations.containsKey(keyPair)) return cachedSourceConfigurations[keyPair]!!

        val scheduleConfig = getScheduleConfiguration(schedule)

        var sourceMapping: SourceMapping? = null

        if (scheduleConfig?.mapping != null) {
            sourceMapping = scheduleConfig.mapping.sources[source]
            val mappingID = sourceMapping?.mappingID
            if (sourceMapping != null) {
                if (!mappingID.isNullOrEmpty()) {
                    val sourceByMappingID = folderNodeById(mappingID)
                    val sourceConfig = SourceConfiguration(sourceMapping, sourceByMappingID)
                    cachedSourceConfigurations[keyPair] = sourceConfig
                    return sourceConfig
                }
            }
        }

        if (scheduleConfig?.model?.folderNode == null) {
            val sourceConfig = SourceConfiguration(sourceMapping, null)
            cachedSourceConfigurations[keyPair] = sourceConfig
            return sourceConfig
        }

        val folderConfiguration = getSourceConfiguration(modelConfig = scheduleConfig.model.modelConfig, source)
        if (folderConfiguration != null) {

            val sourceConfig = SourceConfiguration(sourceMapping, folderNodeById(folderConfiguration.id))
            cachedSourceConfigurations[keyPair] = sourceConfig
            return sourceConfig
        }

        val browseName = QualifiedName(scheduleConfig.model.folderNode.nodeId.namespaceIndex, source)
        val sourceFolderByBrowseName = folderByBrowseName(browseName)
        val sourceConfig = SourceConfiguration(sourceMapping, sourceFolderByBrowseName)
        cachedSourceConfigurations[keyPair] = sourceConfig
        return sourceConfig
    }

    private fun getSourceConfiguration(modelConfig: FolderNodeConfiguration?, source: String): FolderNodeConfiguration? {
        if (modelConfig == null) return null

        val folderNodeConfig = (modelConfig as DataModelConfiguration).findFolder(source)
        if (folderNodeConfig != null) return folderNodeConfig

        val browseName = QualifiedName(modelConfig.nameSpaceIndex, source)
        val sourceFolderByBrowseName = folderByBrowseName(browseName)
        return sourceFolderByBrowseName?.folderConfig

    }

    private fun getChannelConfiguration(schedule: String, source: String, channel: String): ChannelConfiguration {

        val key = Triple(schedule, source, channel)

        if (cachedChannelConfigurations.containsKey(key)) return cachedChannelConfigurations[key]!!

        val sourceConfiguration = getSourceConfiguration(schedule, source)

        val channelMapping = sourceConfiguration.mapping?.channels?.get(channel)
        if (channelMapping != null && !channelMapping.mappingID.isNullOrEmpty()) {
            val channelConfig = ChannelConfiguration(channelMapping)
            cachedChannelConfigurations[key] = channelConfig
            return channelConfig
        }

        val sourceFolderConfig = sourceConfiguration.folder?.folderConfig
        val channelByIdInFolder = sourceFolderConfig?.variables?.get(channel)
        if (channelByIdInFolder != null) {
            val channelByIdFromFolder = variableById(channelByIdInFolder.id)
            if (channelByIdFromFolder != null) {
                val channelConfig = ChannelConfiguration(channelMapping)
                cachedChannelConfigurations[key] = channelConfig
                return channelConfig
            }
        }

        val scheduleConfig = getScheduleConfiguration(schedule)
        if (scheduleConfig?.model == null) {
            val channelConfig = ChannelConfiguration(channelMapping)
            cachedChannelConfigurations[key] = channelConfig
            return channelConfig
        }

        val channelConfiguration = ChannelConfiguration(channelMapping)
        cachedChannelConfigurations[key] = channelConfiguration
        return channelConfiguration
    }


    private fun getOrCreateFolder(id: String,
                                  parent: UaFolderNode?,
                                  key: String,
                                  displayName: String,
                                  folderMapping: Map<String, DataModelMappingBase>?,
                                  fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        if (parent == null) return null

        if (cachedFolders.containsKey(key)) return cachedFolders[key]

        val mappedToId = folderMapping?.get(id)?.mappingID

        val existingFolder = searchFolder(parent, id, mappedToId)
        if (existingFolder != null) {
            cachedFolders[key] = existingFolder
            return existingFolder
        }

        if (config.autoCreate && mappedToId == null) {
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
            parent = namespaceFolders.first().folderNode,
            displayName = scheduleName.capitalized(),
            key = buildKey(scheduleName = scheduleName),
            fnModelUpdated = fnModelUpdated,
            folderMapping = emptyMap())

        return folder
    }


    fun getSourceFolder(scheduleName: String, sourceName: String, fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        val scheduleConfiguration = getScheduleConfiguration(scheduleName)
        val folder = getOrCreateFolder(
            id = "$scheduleName/$sourceName".lowercase(),
            parent = getScheduleFolder(scheduleName, fnModelUpdated),
            displayName = sourceName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName),
            folderMapping = scheduleConfiguration?.mapping?.sources,
            fnModelUpdated = fnModelUpdated)

        return folder
    }

    fun getChannelFolder(scheduleName: String, sourceName: String, channelName: String, fnModelUpdated: ((UaNode) -> Unit?)?): UaFolderNode? {

        val sourceConfiguration = getSourceConfiguration(scheduleName, sourceName)

        val sourceFolder = getSourceFolder(scheduleName, sourceName, fnModelUpdated)
        val folder = getOrCreateFolder(
            id = buildNodeIdStr(scheduleName, sourceName, channelName),
            displayName = channelName,
            parent = sourceFolder,
            fnModelUpdated = fnModelUpdated,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName),
            folderMapping = sourceConfiguration.mapping?.channels)

        return folder
    }


    fun getChannelVariable(channelFolder: UaFolderNode?,
                           scheduleName: String,
                           sourceName: String,
                           channelName: String,
                           valueName: String = "",
                           value: Any,
                           fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        if (channelFolder == null) return null
        val idStr = buildNodeIdStr(scheduleName, sourceName, channelName, valueName)
        val channelConfiguration = getChannelConfiguration(scheduleName, sourceName, channelName)
        val variableNode = getOrCreateVariable(
            id = idStr,
            folder = channelFolder,
            displayName = (valueName.ifEmpty { channelName }).capitalized(),
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, channelName = channelName, valueName = valueName),
            variableMapping = channelConfiguration.mapping?.metadata,
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }


    private fun searchVariable(folder: UaFolderNode, id: String, mapTo: String?): UaVariableNode? {

        val log = logger.getCtxLoggers(className, "searchVariable")

        if (!mapTo.isNullOrEmpty()) {
            log.trace("Search variable with ID \"$id\" with $CONFIG_MAPPING_ID value of \"$mapTo\"")
            val variableByExplicitMapping = variableById(mapTo)
            if (variableByExplicitMapping == null) {
                log.warning("Unable to find variable with ID \"$id\" with $CONFIG_MAPPING_ID value of \"$mapTo\", check if there are any variable nodes in the model with an id of \"$mapTo\"")
            } else {
                log.trace("Found variable with ID \"$id\" using $CONFIG_MAPPING_ID value of \"$mapTo\", folder is ${variableByExplicitMapping.nodeId.toParseableString()}}")
            }
            return variableByExplicitMapping
        }

        val folderConfiguration = folderByBrowseName(folder.browseName)?.folderConfig
        if (folderConfiguration != null) {
            log.trace("Search variable with ID \"$id\" by ID \"$id\"")
            val variableByIdInFolder = folderConfiguration.variables?.get(id)
            if (variableByIdInFolder != null) {
                val variableByID = variableById(id)
                if (variableByID != null){
                    log.trace("Found variable with ID \"$id\", variable is ${variableByID.nodeId.toParseableString()}}")
                    return variableByID
                } else{
                    log.trace("No variable with ID \"$id\" found")
                }
            }
        }

        val browseName = QualifiedName(folder.nodeId.namespaceIndex, id)
        log.trace("Search variable with ID \"$id\" using browse name ${browseName.toParseableString()}")

        val channelByBrowseName = variableByBrowseName(browseName)
        if (channelByBrowseName != null) {
            log.trace("Found variable with browse name ${browseName.toParseableString()} folder is ${channelByBrowseName.nodeId.toParseableString()}}")
            return channelByBrowseName
        }
        log.trace("No variable found with browseName ${browseName.toParseableString()}")

        return null

    }

    private fun searchFolder(folder: UaFolderNode, id: String, mapTo: String?): UaFolderNode? {

        val log = logger.getCtxLoggers(className, "searchFolder")
        log.trace("Find folder with ID \"$id\" within folder ${folder.nodeId.toParseableString()}")

        if (!mapTo.isNullOrEmpty()) {
            log.trace("Search folder with ID \"$id\" with $CONFIG_MAPPING_ID  value of \"$mapTo\" within folder ${folder.nodeId.toParseableString()} and subfolders")
            val folderByDirectMapping = folderNodeById(mapTo)
            val folderByExplicitMapping = folderByDirectMapping?.folderNode
            if (folderByExplicitMapping == null) {
                log.warning("Unable to find folder with ID \"$id\" with $CONFIG_MAPPING_ID value of \"$mapTo\" within folder ${folder.nodeId.toParseableString()}, check if there are any folder nodes in the model with an id of \"$mapTo\"")
            } else {
                log.trace("Found folder with ID \"$id\" using $CONFIG_MAPPING_ID value of \"$mapTo\", folder is ${folderByExplicitMapping.nodeId.toParseableString()}}")
            }
            return folderByExplicitMapping
        }

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

        val browseName = QualifiedName(folder.nodeId.namespaceIndex, id)

        log.trace("Search folder with ID \"$id\" using browse name ${browseName.toParseableString()}")
        val folderByBrowseName = folderByBrowseName(browseName)
        if (folderByBrowseName?.folderNode != null) {
            log.trace("Found folder with browse name ${browseName.toParseableString()}, folder is ${folderByBrowseName.folderNode.nodeId.toParseableString()}}")
            return folderByBrowseName.folderNode
        }
        log.trace("No folder found with browseName ${browseName.toParseableString()}")

        return null
    }

    private fun getOrCreateVariable(id: String,
                                    folder: UaFolderNode,
                                    key: String,
                                    displayName: String,
                                    variableMapping: Map<String, DataModelMappingBase>?,
                                    value: Any,
                                    fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        if (cachedVariables.containsKey(key)) return cachedVariables[key]

        val mappedToID = variableMapping?.get(id)?.mappingID

        val existingNode = searchVariable(folder, id, mappedToID)

        if (existingNode != null && mappedToID == null) {
            cachedVariables[key] = existingNode
            return existingNode
        }

        if (config.autoCreate) {

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
        config.dataModels.values.forEach {

            val ns = OpcuaNamespaceBuilder(server, it, Identifiers.ObjectsFolder.expanded(), attributeFilter, logger)
            log.info("Creating namespace ${ns.namespaceIndex}:${it.id} for model ${it.id}")

            ns.onNamespaceFolderNodeCreated = { modelConfig, uaFolderNode ->
                storeNamespaceFolder(modelConfig, uaFolderNode)
            }

            ns.onVariableNodeCreated = { variableNodeConfig, uaVariableNode ->
                storeVariable(variableNodeConfig.id, uaVariableNode)

            }

            ns.onFolderNodeCreated = { folderNodeConfig, uaFolderNode ->
                storeFolder(folderNodeConfig, uaFolderNode)
            }

            namespaceBuilders[ns.namespaceIndex.toInt()] = ns

            ns.startup()
        }
    }


    fun getScheduleMetadataVariable(scheduleFolder: UaFolderNode, scheduleName: String, metadataName: String, value: Any, fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {

        val scheduleConfiguration = getScheduleConfiguration(scheduleName)
        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, metadataName),
            folder = scheduleFolder,
            displayName = metadataName,
            key = buildKey(scheduleName = scheduleName, metadataName = metadataName),
            variableMapping = scheduleConfiguration?.mapping?.metadata,
            fnModelUpdated = fnModelUpdated,
            value = value)

        return variableNode
    }

    fun getSourceMetadataVariable(sourceFolder: UaFolderNode, scheduleName: String, sourceName: String, metadataName: String, value: Any, fnModelUpdated: ((UaNode) -> Unit?)?): UaVariableNode? {
        val folderConfiguration = getSourceConfiguration(scheduleName, sourceName)
        val variableNode = getOrCreateVariable(
            id = buildNodeIdStr(scheduleName, sourceName, metadataName),
            folder = sourceFolder,
            displayName = metadataName,
            key = buildKey(scheduleName = scheduleName, sourceName = sourceName, metadataName = metadataName),
            variableMapping = folderConfiguration.mapping?.metadata,
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