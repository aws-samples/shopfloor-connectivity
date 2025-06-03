// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.transformations.TransformationOperator
import com.amazonaws.sfc.transformations.TransformationsDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class AwsS3TablesTargetConfigReader(configReader : ConfigReader) : ConfigReader(configReader.config, configReader.allowUnresolved, configReader.secretsManager) {
    override fun createJsonConfigReader(): Gson = GsonBuilder()
        .registerTypeAdapter(TablePartitionConfiguration::class.java, TablePartitionDeserializer())
        .registerTypeAdapter(FieldMappingConfiguration::class.java, FieldMappingDeserializer())
        .registerTypeAdapter(FieldConfiguration::class.java, FieldConfigurationDeserializer())
        .registerTypeAdapter(TransformationOperator::class.java, TransformationsDeserializer())
        .create()
}


