
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcCoreVersion = "1.0.0"

val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"


plugins {

    java

    id("sfc.kotlin-library-conventions")
    `maven-publish`
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")



}

publishing {

    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = group as String
            artifactId = "modbus"
            version = version
        }
    }

}

tasks.build {
    finalizedBy(tasks.publishToMavenLocal)
}
