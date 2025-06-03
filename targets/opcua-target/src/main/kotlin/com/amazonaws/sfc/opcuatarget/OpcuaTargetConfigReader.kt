// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.opcuatarget.config.DataModelConfigurationMap
import com.amazonaws.sfc.opcuatarget.config.FolderNodesConfigurationMap
import com.amazonaws.sfc.opcuatarget.config.VariableNodeMap
import com.amazonaws.sfc.transformations.TransformationOperator
import com.amazonaws.sfc.transformations.TransformationsDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder

// Specific config reader with custom deserializers for model configuration
class OpcuaTargetConfigReader(configReader : ConfigReader) : ConfigReader(configReader.config, configReader.allowUnresolved, configReader.secretsManager) {
    override fun createJsonConfigReader(): Gson = GsonBuilder()
        .registerTypeAdapter(DataModelConfigurationMap::class.java, DataModelConfigurationMap.ModelConfigurationMapDeserializer())
        .registerTypeAdapter(VariableNodeMap::class.java, VariableNodeMap.VariableNodesDeserializer())
        .registerTypeAdapter(FolderNodesConfigurationMap::class.java, FolderNodesConfigurationMap.FolderNodesDeserializer())
        .registerTypeAdapter(TransformationOperator::class.java, TransformationsDeserializer())
        .create()
}
