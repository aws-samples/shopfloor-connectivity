// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

plugins {
    id("java")
}

buildscript {
    val version = (gradle as ExtensionAware).extensions.extraProperties.get("versionFromGit") as String
    extra.apply {
        // SET GLOBAL SFC RELEASE VERSION HERE
        // --> applied to sfc-core, sfc-ipc, sfc-main & sfc-metrics <--
        set("sfc_release", version)
    }
}

tasks.register<Zip>("packageDistribution") {
    archiveFileName.set("sfc-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distribution-bundle"))
    from(layout.buildDirectory.dir("distribution"))
}

tasks.named("assemble") {
    finalizedBy("packageDistribution")
}