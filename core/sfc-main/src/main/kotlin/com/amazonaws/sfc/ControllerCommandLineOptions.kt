
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc

import com.amazonaws.sfc.service.CommandLine
import org.apache.commons.cli.Options

class ControllerCommandLineOptions(args: Array<String>) : CommandLine(args) {

    override fun options(): Options {
        val options = super.options()
        options.addOption(configOption.required(true).build())
        options.addOption(configVerificationPublicKeyFile)
        return options
    }

    val configFilename: String by lazy {
        cmd.getOptionValue(OPTION_CONFIG_FILE)
    }

    val configVerificationPublicKeyFileName: String? by lazy {
        cmd.getOptionValue(OPTION_CONFIG_VERIFY_PUBLIC_KEY_FILE)

    }


}