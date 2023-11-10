// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

plugins {
    id("java")
}

tasks.register<Zip>("packageDistribution") {
    archiveFileName.set("sfc-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distribution-bundle"))
    from(layout.buildDirectory.dir("distribution"))
}

tasks.named("assemble") {
    finalizedBy("packageDistribution")
}