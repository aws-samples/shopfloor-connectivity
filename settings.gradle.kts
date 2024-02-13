// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

rootProject.name = "sfc"

pluginManagement {
    fun String.runCommand(
        workingDir: File = File("."),
        timeoutAmount: Long = 60,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS
    ): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply { waitFor(timeoutAmount, timeoutUnit) }
        .run {
            val error = errorStream.bufferedReader().readText().trim()
            if (error.isNotEmpty()) {
                throw Exception(error)
            }
            inputStream.bufferedReader().readText().trim()
        }
    val versionFromGit = "git describe --tags --abbrev=0".runCommand(workingDir = rootDir).replace("v","")
    (gradle as ExtensionAware).extensions.extraProperties.set("versionFromGit", versionFromGit)
}

//-----------------//

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

listOf("core", "metrics", "adapters", "targets", "examples").forEach { p ->
    File("$rootDir/$p/").listFiles()?.forEach {
        if (it.isDirectory && File(it, "build.gradle.kts").exists()) {
            include(":${p}:${it.name}")
        }
    }
}








