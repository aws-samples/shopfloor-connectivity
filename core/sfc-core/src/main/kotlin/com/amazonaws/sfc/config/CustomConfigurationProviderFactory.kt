
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

/**
 * Factory class to create in process instances for handling custom configurations
 */
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.InstanceFactory
import java.security.PublicKey


@ConfigurationClass
class CustomConfigurationProviderFactory(config: InProcessConfiguration, private val configVerificationKey: PublicKey?, private val logger: Logger) {
    private val factory = InstanceFactory<ConfigProvider>(config, logger)
    fun newProviderInstance(c: String, instanceLogger: Logger = logger) = factory.createInstance(c, configVerificationKey, instanceLogger)
}

