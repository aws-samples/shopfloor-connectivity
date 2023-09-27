import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

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

val module = "router"
val sfcCoreVersion = "1.0.0"
val sfcIpcVersion = "1.0.0"

val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val gsonVersion = "2.9.0"
val junitVersion = "5.6.0"
val jvmTarget = "1.8"

repositories {
    mavenCentral()
}

plugins {
    application
    kotlin("jvm") version "1.8.10"
    java
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))
    implementation("com.google.code.gson:gson:$gsonVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass.set("com.amazonaws.sfc.router.AwsRouterTargetService")
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
}

tasks.compileJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.compileTestJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}




