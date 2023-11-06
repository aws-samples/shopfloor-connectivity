
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.service.ServerConnectionType
import com.amazonaws.sfc.service.ServiceCommandLineOptions
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options

open class IpcServiceCommandLine(args: Array<String>) : ServiceCommandLineOptions(args) {

    val port: Int?
        get() {

            var port: Int? = (cmd.getParsedOptionValue(OPTION_PORT) as Long?)?.toInt()

            if (port != null) {
                return port
            }

            if (cmd.hasOption(ENV_OPTION_PORT)) {
                port = (System.getenv(cmd.getOptionValue(ENV_OPTION_PORT)) ?: "").toIntOrNull()
                if (port != null) {
                    return port
                }
            }
            return null
        }


    val key: String?
        get() = cmd.getOptionValue(OPTION_KEY_FILE)

    val ca: String?
        get() = cmd.getOptionValue(OPTION_CA_FILE)

    val cert: String?
        get() = cmd.getOptionValue(OPTION_CERTIFICATE_FILE)

    val connectionType: ServerConnectionType?
        get() = try {
            val connectionTypeParameter: String? = cmd.getOptionValue(OPTION_CONNECTION_TYPE)
            if (connectionTypeParameter != null) ServerConnectionType.valueOf(connectionTypeParameter) else ServerConnectionType.PlainText
        } catch (_: Exception) {
            ServerConnectionType.Unknown
        }

    val networkInterface: String?
        get() = cmd.getOptionValue(OPTION_NETWORK_INTERFACE)

    override fun options(): Options {

        val options = super.options()
        val portOrConfig = OptionGroup()
        portOrConfig.addOption(configOption.required(false).build())
        portOrConfig.addOption(portOption)
        portOrConfig.addOption(envPortOption)
        options.addOptionGroup(portOrConfig)


        return options
    }

}


class ProtocolServerCommandLine(args: Array<String>) : IpcServiceCommandLine(args) {

    val protocolAdapterID: String? = cmd.getOptionValue(OPTION_PROTOCOL_ADAPTER)

    override fun options(): Options {

        val options = super.options()
        options.addOption(protocolOption)

        return options
    }

    companion object {

        const val OPTION_PROTOCOL_ADAPTER = "adapter"

        private val protocolOption: Option = Option.builder(OPTION_PROTOCOL_ADAPTER)
            .type(String::class.java)
            .argName(OPTION_PROTOCOL_ADAPTER)
            .desc("Adapter ID for the service")
            .hasArg()
            .build()
    }


}