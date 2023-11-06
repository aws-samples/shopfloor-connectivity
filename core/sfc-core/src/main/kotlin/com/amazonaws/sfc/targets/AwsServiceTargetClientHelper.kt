
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.targets

import com.amazonaws.sfc.client.AwsServiceClientHelper
import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.log.Logger
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder

/**
 * Helper class for executing and retrying AWS service calls.
 * @param config C : Configuration instance.
 * @param targetID String Identifier of the target in the configuration
 * @param builder AwsClientBuilder<*, *> Instance of a client builder for the used AWS service.
 * @param logger Logger Logger for output
 * @constructor
 */
class AwsServiceTargetClientHelper(
    private val config: HasCredentialClients,
    private val targetID: String,
    private val builder: AwsClientBuilder<*, *>,
    private val logger: Logger) : AwsServiceClientHelper(config, builder, logger) {

    // Gets the configuration for the target from the configuration
    override val awsService: AwsServiceConfig by lazy {
        // get target configuration
        val cc = config as AwsServiceTargetsConfig<*>
        cc.targets[targetID]
        ?: throw ConfigurationException("Configuration for target \"$targetID\" does not exist, configured targets are ${config.targets.keys}", CONFIG_TARGETS)
    }


    /**
     * Loads a target configuration
     * @param configReader ConfigReader
     * @param targetType String
     * @return T configuration
     */
    inline fun <reified T : BaseConfiguration> writerConfig(configReader: ConfigReader, targetType: String): T {
        try {
            return configReader.getConfig()
        } catch (e: Exception) {
            throw ConfigurationException("Could not load $targetType Target configuration: ${e.message}", CONFIG_TARGETS)
        }
    }

    /**
     * Retrieves the target configuration for the specified target ID
     * @param cf AwsServiceTargetsConfig<T> Source Target writer configuration
     * @param targetID String ID of the target
     * @param configType String Name of the target type
     * @return T Target configuration
     */
    inline fun <reified T : AwsServiceConfig> targetConfig(cf: AwsServiceTargetsConfig<T>, targetID: String, configType: String): T {
        // get target configuration
        return cf.targets[targetID]
               ?: throw ConfigurationException("Configuration for type $configType for target with ID \"$targetID\" does not exist, existing targets are ${cf.targets.keys}", CONFIG_TARGETS)
    }
}