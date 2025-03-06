// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuatarget.OpcuaServerDataTypes.Companion.toVariant
import com.amazonaws.sfc.opcuatarget.config.DataModelConfiguration
import com.amazonaws.sfc.opcuatarget.config.FolderNodeConfiguration
import com.amazonaws.sfc.opcuatarget.config.VariableNodeConfiguration
import org.eclipse.milo.opcua.sdk.core.AccessLevel
import org.eclipse.milo.opcua.sdk.core.Reference
import org.eclipse.milo.opcua.sdk.server.Lifecycle
import org.eclipse.milo.opcua.sdk.server.OpcUaServer
import org.eclipse.milo.opcua.sdk.server.api.DataItem
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode.UaVariableNodeBuilder
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger


class OpcuaNamespaceBuilder(server: OpcUaServer,
                            private val modelConfiguration: DataModelConfiguration,
                            private val root: ExpandedNodeId = Identifiers.ObjectsFolder.expanded(),
                            private val attributeFilter: AttributeFilter?,
                            private val initializeValuesWithNull : Boolean,
                            private val logger: Logger) : ManagedNamespaceWithLifecycle(server, modelConfiguration.nameSpace) {

    private val className = this::class.java.simpleName

    init {
        modelConfiguration.nameSpaceIndex = namespaceIndex.toInt()
        val serverNode = server.addressSpaceManager.getManagedNode(Identifiers.Server).get()
        if (serverNode is ServerTypeNode) serverNode.eventNotifier = UByte.valueOf(1)
    }

    private var _onNamespaceFolderNodeCreated: ((DataModelConfiguration, UaFolderNode) -> Unit)? = null
    var onNamespaceFolderNodeCreated: ((DataModelConfiguration, UaFolderNode) -> Unit)?
        get() = _onNamespaceFolderNodeCreated
        set(value) {
            _onNamespaceFolderNodeCreated = value
        }

    private var _onFolderNodeCreated: ((FolderNodeConfiguration, UaFolderNode) -> Unit)? = null
    var onFolderNodeCreated: ((FolderNodeConfiguration, UaFolderNode) -> Unit)?
        get() = _onFolderNodeCreated
        set(value) {
            _onFolderNodeCreated = value
        }

    private var _onVariableNodeCreated: ((VariableNodeConfiguration, UaVariableNode) -> Unit)? = null
    var onVariableNodeCreated: ((VariableNodeConfiguration, UaVariableNode) -> Unit)?
        get() = _onVariableNodeCreated
        set(value) {
            _onVariableNodeCreated = value
        }


    private val subscriptionModel = SubscriptionModel(server, this)

    init {

        lifecycleManager.addLifecycle(subscriptionModel)

        lifecycleManager.addStartupTask { this.createAndAddNodes() }

        lifecycleManager.addLifecycle(object : Lifecycle {

            override fun startup() {
            }

            override fun shutdown() {
                try {
                } catch (_: InterruptedException) {
                }
            }
        })
    }


    fun addFolderNode(nodeConfig: FolderNodeConfiguration, parent: ExpandedNodeId): UaFolderNode {

        val log = logger.getCtxLoggers(className, "addFolderNode")

        val folderNode = UaFolderNode(nodeContext, nodeConfig.nodeID, newQualifiedName(nodeConfig.browseName), LocalizedText(nodeConfig.displayName))
        folderNode.description = LocalizedText(nodeConfig.description)
        nodeManager.addNode(folderNode)

        folderNode.addReference((Reference(folderNode.nodeId, Identifiers.Organizes, parent, false)))
        onFolderNodeCreated?.invoke(nodeConfig, folderNode)

        log.trace("Created folder node ${nodeConfig.nodeID?.toParseableString()}, Display name: \"${nodeConfig.displayName}\", Browse name: \"${nodeConfig.browseName}\", within folder ${parent.toParseableString()}")

        nodeConfig.variables?.values?.forEach { variableConfig ->
            addVariableNode(variableConfig, folderNode)
        }

        nodeConfig.folders?.values?.forEach { folder ->
            addFolderNode(folder, folderNode.nodeId.expanded())
        }


        return folderNode

    }

    fun addVariableNode(nodeConfig: VariableNodeConfiguration, folder: UaFolderNode): UaVariableNode {

        val log = logger.getCtxLoggers(className, "addVariableNode")

        val variableNodeBuilder = UaVariableNodeBuilder(nodeContext)

        variableNodeBuilder
            .setNodeId(nodeConfig.nodeID)
            .setAccessLevel(AccessLevel.READ_ONLY)
            .setBrowseName(QualifiedName(namespaceIndex, nodeConfig.browseName))
            .setDisplayName(LocalizedText(nodeConfig.displayName))
            .setDataType(nodeConfig.dataTypeIdentifier)
            .setTypeDefinition(Identifiers.BaseVariableType)
            .setMinimumSamplingInterval(0.0)
            .setHistorizing(false)


        if (nodeConfig.arrayDimensions != null) {
            val dimensions = nodeConfig.arrayDimensions.map { UInteger.valueOf(it.toLong()) }.toTypedArray()
            variableNodeBuilder.arrayDimensions = dimensions
            variableNodeBuilder.valueRank = dimensions.size
        }

        if (nodeConfig.description != null) {
            variableNodeBuilder.description = LocalizedText(nodeConfig.description)
        }

        if (attributeFilter != null) variableNodeBuilder.addAttributeFilter(attributeFilter)

        val variableNode = variableNodeBuilder.build()

        nodeManager.addNode(variableNode)
        onVariableNodeCreated?.invoke(nodeConfig, variableNode)

        if (nodeConfig.initValue != null ) {
            log.trace("Setting configured initial value for ${nodeConfig.nodeID} to ${nodeConfig.initValue}")
            variableNode.value = DataValue(nodeConfig.initValue.toVariant(nodeConfig.dataTypeIdentifier, nodeConfig.arrayDimensions, logger))
        } else {
            if (initializeValuesWithNull) {
                log.trace("Setting initial value for ${nodeConfig.nodeID} to null")
                variableNode.value = DataValue(null.toVariant(nodeConfig.dataTypeIdentifier, nodeConfig.arrayDimensions, logger))
            }
        }

        folder.addOrganizes(variableNode)

        log.trace(
            "Created variable node ${nodeConfig.nodeID?.toParseableString()}, Display name: \"${nodeConfig.displayName}\", Browse name: \"${nodeConfig.browseName}\", Datatype: ${nodeConfig.dataType}" +
                    if (!nodeConfig.arrayDimensions.isNullOrEmpty()) ", Array dimensions: ${nodeConfig.arrayDimensions.joinToString()}" else "" +
                            if (nodeConfig.initValue != null) ", Init value: ${nodeConfig.initValue}" else "" +
                                    ", within folder node ${folder.nodeId.toParseableString()}"
        )


        return variableNode
    }


    private fun createAndAddNodes() {

        modelConfiguration.nameSpaceIndex = namespaceIndex.toInt()
        val modelFolderNode = addFolderNode(modelConfiguration, root)

        onNamespaceFolderNodeCreated?.invoke(modelConfiguration, modelFolderNode)
    }

    override fun onDataItemsCreated(dataItems: List<DataItem>) {
        subscriptionModel.onDataItemsCreated(dataItems)
    }

    override fun onDataItemsModified(dataItems: List<DataItem>) {
        subscriptionModel.onDataItemsModified(dataItems)
    }

    override fun onDataItemsDeleted(dataItems: List<DataItem>) {
        subscriptionModel.onDataItemsDeleted(dataItems)
    }

    override fun onMonitoringModeChanged(monitoredItems: List<MonitoredItem>) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems)
    }


}