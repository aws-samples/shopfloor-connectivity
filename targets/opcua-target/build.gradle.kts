// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "opcuatarget"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val miloVersion = "0.6.14"
val gsonVersion = "2.9.0"

plugins {
    id("sfc.kotlin-application-conventions")
    java
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.eclipse.milo:opc-ua-sdk:$miloVersion")
    implementation("org.eclipse.milo:sdk-server:$miloVersion")
    api("com.google.code.gson:gson:$gsonVersion")
}

application {
    mainClass.set("com.amazonaws.sfc.opcuatarget.OpcuaTargetService")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.distTar {
    project.version = version
    archiveBaseName = "${project.name}"
    compression = Compression.GZIP
    archiveExtension = "tar.gz"
    archiveFileName = "${project.name}.tar.gz"
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
          |package com.amazonaws.sfc.$module
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

