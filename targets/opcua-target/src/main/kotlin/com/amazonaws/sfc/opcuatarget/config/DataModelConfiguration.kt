// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.amazonaws.sfc.config.Validate
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject


class DataModelConfiguration(folderConfig: FolderNodeConfiguration, namespace: String)
    : FolderNodeConfiguration(folderConfig, folderConfig.folders, folderConfig.variables), ParentNode, Validate {

    constructor(namespaceIndex : Int, id: String,
                nameSpace: String,
                browseName: String? = null,
                displayName: String? = null,
                description: String? = null,
                folders: FolderNodesConfigurationMap = FolderNodesConfigurationMap(),
                variables: VariableNodeMap = VariableNodeMap()) :
            this(FolderNodeConfiguration(namespaceIndex, id, browseName, displayName, description, folders, variables), nameSpace)


    private var _nameSpace: String = namespace
    val nameSpace: String
        get() = _nameSpace

    override fun findVariable(id : String) : VariableNodeConfiguration?{
        return (this as FolderNodeConfiguration).findVariable(id)
    }

    override fun findFolder(id : String) : FolderNodeConfiguration?{
        this.folders?.values?.forEach {
            if (it.id == id) return it
            val sub =  it.findFolder(id)
            if (sub != null) return sub
        }
        return null
    }


    companion object {
        fun fromJson(key: String, jsonObject: JsonObject, context: JsonDeserializationContext?): DataModelConfiguration {

            val folderConfig = FolderNodeConfiguration.fromJson(key, jsonObject, context)

            return DataModelConfiguration(
                folderConfig = folderConfig,
                namespace = (jsonObject[key]?.asJsonObject?.get(CONFIG_MODEL_NAMESPACE)?.asString) ?: "",
            )

        }

        private const val CONFIG_MODEL_NAMESPACE = "Namespace"
        private const val DEFAULT_NAMESPACE = "urn:amazonaws.sfc"
        private const val DEFAULT_DATA_MODEL_ID = "SFC"
        val DEFAULT_SFC_DATA_MODEL = DataModelConfiguration(namespaceIndex = 0,id=DEFAULT_DATA_MODEL_ID, nameSpace = DEFAULT_NAMESPACE)

    }

}