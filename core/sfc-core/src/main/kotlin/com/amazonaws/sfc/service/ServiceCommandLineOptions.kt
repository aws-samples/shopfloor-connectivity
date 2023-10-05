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

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

open class ServiceCommandLineOptions(args: Array<String>) : CommandLine(args) {


    override fun options(): Options {
        val options = super.options()
        options.addOption(portOption)
        options.addOption(keyFileOption)
        options.addOption(certificateFileOption)
        options.addOption(networkInterfaceOption)
        options.addOption(caFileOption)
        options.addOption(connectionTypeOption)
        return options
    }

    companion object {

        const val OPTION_CERTIFICATE_FILE = "cert"
        const val OPTION_CA_FILE = "ca"
        const val OPTION_KEY_FILE = "key"
        const val OPTION_PORT = "port"
        const val ENV_OPTION_PORT = "envport"
        const val OPTION_NETWORK_INTERFACE = "interface"
        const val OPTION_CONNECTION_TYPE = "connection"


        val portOption: Option = Option.builder(OPTION_PORT)
            .type(Number::class.java)
            .argName(OPTION_PORT)
            .desc("Port number for the service")
            .hasArg()
            .build()

        val connectionTypeOption: Option = Option.builder(OPTION_CONNECTION_TYPE)
            .type(ServerConnectionType::class.java)
            .argName(OPTION_CONNECTION_TYPE)
            .desc("Connection type (${ServerConnectionType.validValues.joinToString(separator = ", ")})")
            .optionalArg(true)
            .hasArg()
            .build()

        val envPortOption: Option = Option.builder(ENV_OPTION_PORT)
            .type(String::class.java)
            .argName(ENV_OPTION_PORT)
            .desc("Environment variable name for port number for the service")
            .hasArg()
            .build()

        private val keyFileOption: Option = Option.builder(OPTION_KEY_FILE)
            .type(String::class.java)
            .argName(OPTION_KEY_FILE)
            .desc("Key file")
            .hasArg()
            .build()

        private val networkInterfaceOption: Option = Option.builder(OPTION_NETWORK_INTERFACE)
            .type(String::class.java)
            .argName(OPTION_NETWORK_INTERFACE)
            .desc("Network interface name")
            .hasArg()
            .build()

        private val certificateFileOption: Option = Option.builder(OPTION_CERTIFICATE_FILE)
            .type(String::class.java)
            .argName(OPTION_CERTIFICATE_FILE)
            .desc("Certificate file")
            .hasArg()
            .build()

        private val caFileOption: Option = Option.builder(OPTION_CA_FILE)
            .type(String::class.java)
            .argName(OPTION_CA_FILE)
            .desc("CA Certificate file")
            .hasArg()
            .build()
    }
}