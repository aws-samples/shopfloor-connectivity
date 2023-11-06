
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.service.ServiceCommandLineOptions
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options

class IpcMetricsServerCommandLine(args: Array<String>) : ServiceCommandLineOptions(args) {

    val port: Int?
        get() {
            if (cmd.hasOption(OPTION_PORT)) {
                return (cmd.getParsedOptionValue(OPTION_PORT) as Long?)?.toInt()
            }

            if (cmd.hasOption(ENV_OPTION_PORT)) {
                val port = (System.getenv(cmd.getOptionValue(ENV_OPTION_PORT)) ?: "").toIntOrNull()
                if (port != null) {
                    return port
                }
            }

            return null
        }


    val key: String? = cmd.getOptionValue(OPTION_KEY_FILE)

    val cert: String? = cmd.getOptionValue(OPTION_CERTIFICATE_FILE)

    val networkInterface: String? = cmd.getOptionValue(OPTION_NETWORK_INTERFACE)


    override fun options(): Options {
        val options = super.options()

        val portOrConfig = OptionGroup()
        portOrConfig.addOption(configOption.required(false).build())
        portOrConfig.addOption(portOption)
        portOrConfig.addOption(envPortOption)
        options.addOptionGroup(portOrConfig)
        options.addOption(targetOption)

        return options
    }

    companion object {

        private const val OPTION_TARGET = "target"

        private val targetOption: Option = Option.builder(OPTION_TARGET)
            .type(String::class.java)
            .argName(OPTION_TARGET)
            .desc("Target ID for the service")
            .hasArg()
            .build()
    }


}