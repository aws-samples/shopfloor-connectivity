/*
 * Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is located at :
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

group = "com.amazonaws.sfc"
version = "1.0.0"

val kotlinVersion = "1.9.0"
val junitVersion = "5.6.0"
val jvmTarget = "1.8"
val sfcCoreVersion = "1.0.0"


repositories {
    mavenCentral()
    mavenLocal {
        url = uri("file://tmp/repo")
    }
}

plugins {
    application
    kotlin("jvm") version "1.8.10"
    java
}

dependencies {

    implementation("com.amazonaws.sfc:sfc-core:$sfcCoreVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass.set("com.amazonaws.sfc.SignConfig")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.getByName<Tar>("distTar").archiveFileName.set("${project.name}.tar")

kotlin {
    jvmToolchain(8)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmTarget
        freeCompilerArgs = listOf("-opt-in=kotlin.time.ExperimentalTime", "-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-source", jvmTarget, "-target", jvmTarget))
}


tasks.compileJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.compileTestJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}









