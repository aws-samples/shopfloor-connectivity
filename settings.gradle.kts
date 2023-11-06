
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0




rootProject.name = "sfc"

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




