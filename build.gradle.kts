// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

plugins {
    id("java")
}

buildscript {
    extra.apply {
        // SET GLOBAL SFC RELEASE VERSION HERE -> applied to all modules
        set("sfc_release", "1.0.3")
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