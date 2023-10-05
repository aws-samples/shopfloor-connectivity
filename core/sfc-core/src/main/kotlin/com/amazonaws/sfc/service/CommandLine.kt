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

import com.amazonaws.sfc.log.LogLevel
import org.apache.commons.cli.*
import org.apache.commons.cli.CommandLine
import kotlin.system.exitProcess

abstract class CommandLine(val args: Array<String>) {

    protected val cmd = commandLine(args)

    private fun commandLine(args: Array<String>): CommandLine {

        val cmd = DefaultParser().parse(options(), args)
        if (cmd.hasOption(OPTION_HELP)) {
            printHelp()
            exitProcess(0)
        }
        return cmd
    }

    open fun options(): Options = commonOptions()

    private fun printHelp() {
        val helpFormatter = HelpFormatter()
        helpFormatter.width = 132
        helpFormatter.printHelp(" ", options())
    }


    val logLevel: LogLevel?
        get() {
            var logLevel: LogLevel? = null
            for ((option, level) in mapOf(
                OPTION_LOGLEVEL_ERROR to LogLevel.ERROR,
                OPTION_LOGLEVEL_WARNING to LogLevel.WARNING,
                OPTION_LOGLEVEL_INFO to LogLevel.INFO,
                OPTION_LOGLEVEL_TRACE to LogLevel.TRACE
            )) {
                if (cmd.hasOption(option)) {
                    logLevel = level
                    break
                }
            }
            return logLevel
        }

    companion object {

        const val OPTION_CONFIG_FILE = "config"
        const val OPTION_CONFIG_VERIFY_PUBLIC_KEY_FILE = "verify"
        private const val OPTION_HELP = "help"
        private const val OPTION_LOGLEVEL_ERROR = "error"
        private const val OPTION_LOGLEVEL_INFO = "info"
        private const val OPTION_LOGLEVEL_TRACE = "trace"
        private const val OPTION_LOGLEVEL_WARNING = "warning"


        private val helpOption: Option = Option.builder("h")
            .argName(OPTION_HELP)
            .longOpt(OPTION_HELP)
            .desc("Displays this help")
            .build()

        val configOption: Option.Builder = Option.builder(OPTION_CONFIG_FILE)
            .type(String::class.java)
            .argName(OPTION_CONFIG_FILE)
            .desc("Name of the configuration file")
            .hasArg()

        val configVerificationPublicKeyFile: Option = Option.builder(OPTION_CONFIG_VERIFY_PUBLIC_KEY_FILE)
            .type(String::class.java)
            .argName(OPTION_CONFIG_VERIFY_PUBLIC_KEY_FILE)
            .desc("Public key file for configuration verification")
            .hasArg(true)
            .required(false)
            .build()

        private val traceLevelOption: Option = Option.builder(OPTION_LOGLEVEL_TRACE)
            .argName(OPTION_LOGLEVEL_TRACE)
            .desc("Enable trace level logging")
            .hasArg(false)
            .build()

        private val infoLogLevel: Option = Option.builder(OPTION_LOGLEVEL_INFO)
            .argName(OPTION_LOGLEVEL_INFO)
            .desc("Enable info level logging")
            .hasArg(false)
            .build()

        private val warningLogLevel: Option = Option.builder(OPTION_LOGLEVEL_WARNING)
            .argName(OPTION_LOGLEVEL_WARNING)
            .desc("Enable warning level logging")
            .hasArg(false)
            .build()

        private val errorLogLevel: Option = Option.builder(OPTION_LOGLEVEL_ERROR)
            .argName(OPTION_LOGLEVEL_ERROR)
            .desc("Enable error level logging")
            .hasArg(false)
            .build()


        fun commonOptions(): Options {

            val options = Options()
            options.addOption(helpOption)
            val logLevels = OptionGroup()
            logLevels.addOption(traceLevelOption)
            logLevels.addOption(infoLogLevel)
            logLevels.addOption(warningLogLevel)
            logLevels.addOption(errorLogLevel)
            options.addOptionGroup(logLevels)

            return options
        }
    }
}