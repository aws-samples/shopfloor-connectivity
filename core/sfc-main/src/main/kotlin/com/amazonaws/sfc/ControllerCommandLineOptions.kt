/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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