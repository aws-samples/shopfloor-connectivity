
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


group = "com.amazonaws.sfc"
version = "1.0.0"

val kotlinVersion = "1.9.0"


val sfcCoreVersion = "1.0.0"

plugins {
    id("sfc.kotlin-application-conventions")

    java
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

}

application {
    mainClass.set("com.amazonaws.sfc.SignConfig")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.getByName<Tar>("distTar").archiveFileName.set("${project.name}.tar")

