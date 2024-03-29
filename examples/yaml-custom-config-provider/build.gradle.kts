// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val kotlinCoroutinesVersion = "1.6.2"
val module = "yaml-custom-config-provider"

plugins {

    java
    application
    id("sfc.kotlin-library-conventions")
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
}


tasks.getByName<Zip>("distZip").enabled = false
tasks.distTar {
    project.version = ""
    archiveBaseName = module
    compression = Compression.GZIP
    archiveExtension = "tar.gz"
}


tasks.register<Copy>("copyDist") {
    from(layout.buildDirectory.dir("distributions"))
    include("*.tar.gz")
    into(layout.buildDirectory.dir("../../../build/distribution/"))
}

task("generateBuildConfig") {
    val version = project.version.toString()
    val versionSource = resources.text.fromString(
        """
          |package com.amazonaws.sfc.config
          |
          |object BuildConfig {
          |  const val CORE_VERSION = "$sfcCoreVersion" 
          |  const val IPC_VERSION = "$sfcIpcVersion"
          |  const val VERSION = "$version"
          |    override fun toString() = "SFC_MODULE ${project.name.toUpperCaseAsciiOnly()}: VERSION=${'$'}VERSION, SFC_CORE_VERSION=${'$'}CORE_VERSION, SFC_IPC_VERSION=${'$'}IPC_VERSION, BUILD_DATE=${LocalDate.now()}"
          |}
          |
        """.trimMargin()
    )

    copy {
        from(versionSource)
        into("src/main/kotlin/com/amazonaws/sfc/$module")
        rename { "BuildConfig.kt" }
    }
}

tasks.named("build") {
    dependsOn("generateBuildConfig")
    finalizedBy("copyDist")
}

