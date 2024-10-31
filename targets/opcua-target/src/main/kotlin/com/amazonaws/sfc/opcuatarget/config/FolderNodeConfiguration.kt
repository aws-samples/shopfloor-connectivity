// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject

open class FolderNodeConfiguration(configNode: BaseNodeConfiguration, folders: FolderNodesConfigurationMap? = FolderNodesConfigurationMap(), variables: VariableNodeMap?) :
        BaseNodeConfiguration(
            namespaceIndex = configNode.nameSpaceIndex,
            id = configNode.id,
            browseName = configNode.browseName,
            displayName = configNode.displayName,
            description = configNode.description), ParentNode {

    constructor(namespaceIndex: Int,
                id: String,
                browseName: String? = null,
                displayName: String? = null,
                description: String? = null,
                folders: FolderNodesConfigurationMap = FolderNodesConfigurationMap(),
                variables: VariableNodeMap = VariableNodeMap()) :
            this(BaseNodeConfiguration(namespaceIndex, id, browseName, displayName, description), folders, variables)

    private val _folders: FolderNodesConfigurationMap? = folders

    override val folders: FolderNodesConfigurationMap?
        get() = _folders

    private val _variables: VariableNodeMap? = variables

    override val variables: VariableNodeMap?
        get() = _variables


    init {
        variables?.values?.forEach { it.parent = this }
        folders?.values?.forEach { it.parent = this }
    }

    override var nameSpaceIndex: Int
        get() = _nameSpaceIndex
        set(value) {
            if (value != _nameSpaceIndex) {
                _nameSpaceIndex = value
                folders?.values?.forEach { it.nameSpaceIndex = value }
                variables?.values?.forEach { it.nameSpaceIndex = value }
            }
        }

    open fun findFolder(id: String): FolderNodeConfiguration? {
        if (this.id == id) return this

        folders?.values?.forEach {
            val sub = it.findFolder(id)
            if (sub != null) return sub
        }
        return null

    }

    open fun findVariable(id: String): VariableNodeConfiguration? {

        this.variables?.values?.forEach { v ->
            if (v.id == id) return v
        }

        this.folders?.values?.forEach {
            it.variables?.values?.forEach { v ->
                if (v.id == id) return v
            }
        }
        return null
    }


    companion object {

        private const val CONFIG_NODE_FOLDERS = "Folders"
        private const val CONFIG_NODE_VARIABLES = "Variables"

        // need explicit function to deserialize as fromJson can not be called recursively
        private fun foldersFromJson(json: JsonObject, context: JsonDeserializationContext?): FolderNodesConfigurationMap {
            val folders = context?.deserialize<FolderNodesConfigurationMap>(json.asJsonObject, FolderNodesConfigurationMap::class.java)
            return folders ?: FolderNodesConfigurationMap()
        }

        private fun variablesFromJson(json: JsonObject, context: JsonDeserializationContext?): VariableNodeMap? {
            val variables = context?.deserialize<VariableNodeMap>(json.asJsonObject, VariableNodeMap::class.java)
            return variables
        }

        @JvmStatic
        fun fromJson(key: String, json: JsonObject, context: JsonDeserializationContext?): FolderNodeConfiguration {
            val configNode = fromJson(key, json)

            val subFoldersJson = json.getAsJsonObject().get(CONFIG_NODE_FOLDERS)
            val subFolders = if (subFoldersJson != null) foldersFromJson(subFoldersJson.asJsonObject, context) else null

            val variablesJson = json.getAsJsonObject().get(CONFIG_NODE_VARIABLES) as JsonObject?
            val variables = if (variablesJson != null) variablesFromJson(variablesJson, context) else null

            val folderNode = FolderNodeConfiguration(configNode, folders = subFolders, variables = variables)
            return folderNode
        }


    }

}