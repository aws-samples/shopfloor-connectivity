// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "mqtt"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val reflectionVersion = "1.6.0"
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val log4jVersion = "2.17.2"
val pahoVersion = "1.2.4"

plugins {
    id("sfc.kotlin-application-conventions")
    java
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$reflectionVersion")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:$pahoVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
}

application {
    mainClass.set("com.amazonaws.sfc.mqtt.MqttTargetService")
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

