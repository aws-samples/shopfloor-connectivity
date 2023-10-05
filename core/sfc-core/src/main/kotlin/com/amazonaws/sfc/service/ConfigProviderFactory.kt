/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.service

import com.amazonaws.sfc.crypto.KeyHelpers
import com.amazonaws.sfc.log.Logger
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File
import java.security.PublicKey
import kotlin.system.exitProcess

/**
 * Factory class for creating instances of know configuration providers based on passed arguments
 */
object ConfigProviderFactory {

    private val className = this::class.java.simpleName

    private fun commandLine(args: Array<String>) =
        DefaultParser()
            .parse(Options()
                .addOption(CommandLine.configOption.build())
                .addOption(CommandLine.configVerificationPublicKeyFile),
                args, true)

    /**
     * Factory method that creates configuration provided using the passed arguments
     * @param args Array<String> arguments
     * @param logger Logger logger used by configuration provider
     * @return ConfigProvider? Created provider, null if no provider could be created from data in arguments
     */
    fun createProvider(args: Array<String>, logger: Logger): ConfigProvider? {
        val cmd = commandLine(args)
        // when clause to determine which provider to create
        return when {

            cmd.hasOption(CommandLine.OPTION_CONFIG_FILE) -> {
                logger.getCtxInfoLog(className, "createProvider")("Creating configuration provider of type ${ConfigProvider::class.java.simpleName}")

                val configFileName = cmd.getOptionValue(CommandLine.OPTION_CONFIG_FILE)

                val configVerificationKey = getConfigurationVerificationKey(cmd, logger)

                if (configFileName != null) ConfigFileProvider(File(configFileName), configVerificationKey, logger) else null
            }

            else -> {
                null
            }

        }
    }

    private fun getConfigurationVerificationKey(cmd: org.apache.commons.cli.CommandLine, logger: Logger): PublicKey? {

        val log = logger.getCtxLoggers(this::class.java.name, "getConfigurationVerificationKey")
        val configVerificationKeyFileName = cmd.getOptionValue(CommandLine.OPTION_CONFIG_VERIFY_PUBLIC_KEY_FILE)
        val configVerificationKeyFile = if (configVerificationKeyFileName != null) File(configVerificationKeyFileName) else null

        return if (configVerificationKeyFile == null) null else
            try {
                if (configVerificationKeyFile.exists()) {
                    log.trace("Loading key from ${configVerificationKeyFile.absoluteFile} to verify configuration")
                    KeyHelpers.loadPublicKey(configVerificationKeyFile)
                } else {
                    log.error("Configuration verification key file ${configVerificationKeyFile.absoluteFile} does not exist")
                    exitProcess((1))
                }

            } catch (e: Exception) {
                log.error("Error loading key from configuration verification key file ${configVerificationKeyFile.absoluteFile}, ${e.message}")
                exitProcess((1))
            }
    }

}