
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc

import com.amazonaws.sfc.config.ConfigVerification
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    checkArguments(args)

    val privateKeyFile = File(args[0])
    val inputConfigFile = File(args[1])
    val signedConfigFile = File(args[2])
    ConfigVerification.sign(inputConfigFile, privateKeyFile, signedConfigFile)
    println("Signed configuration file written to ${signedConfigFile.absoluteFile}")

}

private fun checkArguments(args: Array<String>) {
    if (args.size != 3) {
        println("Usage: sign-sfc-config <private-key-file> <config-file>  <signed-config-file>")
        exitProcess(0)
    }
}

