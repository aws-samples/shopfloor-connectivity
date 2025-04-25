// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

group = "com.amazonaws.sfc"
version = rootProject.extra.get("sfc_release")!!

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "fatjar"
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
    mergedJar("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    mergedJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
}

tasks.jar {
    dependsOn(mergedJar)

    isZip64 = true

    archiveBaseName.set("sfc-fatjar")

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
