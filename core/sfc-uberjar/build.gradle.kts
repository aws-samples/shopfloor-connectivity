// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = rootProject.extra.get("sfc_release")!!

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "uberjar"
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val reflectionVersion = "1.6.0"
val jmesPathVersion = "0.5.1"
val gsonVersion = "2.9.0"

val mergedJar by configurations.creating<Configuration> {
    // we're going to resolve this config here, in this project
    isCanBeResolved = true
    // this configuration will not be consumed by other projects
    //isCanBeConsumed = false
    isCanBeConsumed = true
    // don't make this visible to other projects
    isVisible = false
    //isVisible = true
}

plugins {
    id("sfc.kotlin-application-conventions")
    id("java")
}

dependencies {
    mergedJar(project(":core:sfc-core"))
    mergedJar(project(":core:sfc-main"))
    mergedJar(project(":core:sfc-ipc"))
    for (adapterProject in rootProject.project(":adapters").childProjects.values) {
        project.logger.lifecycle(adapterProject.name)
        mergedJar(adapterProject)
    }
    for (adapterProject in rootProject.project(":targets").childProjects.values) {
        project.logger.lifecycle(adapterProject.name)
        mergedJar(adapterProject)
    }
    for (adapterProject in rootProject.project(":metrics").childProjects.values) {
        project.logger.lifecycle(adapterProject.name)
        mergedJar(adapterProject)
    }
    for (adapterProject in rootProject.project(":examples").childProjects.values) {
        project.logger.lifecycle(adapterProject.name)
        mergedJar(adapterProject)
    }
    mergedJar("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    mergedJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
}

tasks.jar {
    dependsOn(mergedJar)

    isZip64 = true

    archiveBaseName.set("sfc-uberjar")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.amazonaws.sfc.MainController"
    }

    from({
        mergedJar
            .map {
                logger.lifecycle("adding $it")
                zipTree(it).matching {
                    exclude { it.name.endsWith(".SF") }
                }
            }

    })
}

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
          |  const val MODULE_VERSION = "$version"
          |    override fun toString() = "SFC_MODULE ${project.name.toUpperCaseAsciiOnly()}: VERSION=${'$'}MODULE_VERSION, SFC_CORE_VERSION=${'$'}CORE_VERSION, SFC_IPC_VERSION=${'$'}IPC_VERSION, BUILD_DATE=${LocalDate.now()}"
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