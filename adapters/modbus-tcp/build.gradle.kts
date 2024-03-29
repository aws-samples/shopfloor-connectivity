// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "modbus.tcp"
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val modbusVersion = "1.0.0"

plugins {
    id("sfc.kotlin-application-conventions")
    java
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    api(project(":adapters:modbus"))
}

application {
    mainClass.set("com.amazonaws.sfc.modbus.tcp.ModbusTcpProtocolService")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.distTar {
	project.version = ""
	archiveBaseName = "${project.name}"
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
          |package com.amazonaws.sfc.$module
          |
          |object BuildConfig {
          |  const val CORE_VERSION = "$sfcCoreVersion" 
          |  const val IPC_VERSION = "$sfcIpcVersion"
          |  const val VERSION = "$version"
          |    override fun toString() = "SFC_MODULE ${project.name.toUpperCaseAsciiOnly()}: VERSION=${'$'}VERSION, MODBUS_VERSION=$modbusVersion, SFC_CORE_VERSION=${'$'}CORE_VERSION, SFC_IPC_VERSION=${'$'}IPC_VERSION, BUILD_DATE=${LocalDate.now()}"
          |}
          |
        """.trimMargin()
    )

    copy {
        from(versionSource)
        into("src/main/kotlin/com/amazonaws/sfc/${module.replace(".", File.separator)}")
        rename { "BuildConfig.kt" }
    }
}

tasks.named("build") {
    dependsOn("generateBuildConfig")
    finalizedBy("copyDist")
}

