// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

group = "com.amazonaws.sfc"
version = "1.0.1"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val module = "awss3tables"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val sfcCoreVersion = sfcRelease
val sfcIpcVersion = sfcRelease
val awsSdkVersion = "2.29.30"
// need this version for jvm 1,8
var icebergVersion = "1.6.1"
var awcIcebergVersion = "1.9.0"
var parquetVersion = "1.15.1"
var parquetFormatsVersion = "2.11.0"
var hadoopVersion = "3.4.1"
var slf4jVersion = "2.0.17"

plugins {
    id("sfc.kotlin-application-conventions")
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")


    implementation("org.apache.iceberg:iceberg-core:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-parquet:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-data:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-api:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-aws:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-aws-bundle:$awcIcebergVersion")

    implementation("software.amazon.awssdk:s3tables:$awsSdkVersion")
    implementation("software.amazon.awssdk:sts:$awsSdkVersion")
    implementation("software.amazon.awssdk:url-connection-client:$awsSdkVersion")

    implementation("org.apache.parquet:parquet-avro:$parquetVersion")
    implementation("org.apache.parquet:parquet-column:$parquetVersion")
    implementation("org.apache.parquet:parquet-common:$parquetVersion")
    implementation("org.apache.parquet:parquet-encoding:$parquetVersion")

    implementation("org.apache.parquet:parquet-hadoop:$parquetVersion")
    implementation("org.apache.parquet:parquet-format:$parquetFormatsVersion")

    implementation("org.apache.hadoop:hadoop-common:$hadoopVersion")
    implementation("org.apache.hadoop:hadoop-client:$hadoopVersion")

    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    
}

application {
    mainClass.set("com.amazonaws.sfc.awss3.AwsS3TablesTargetService")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.distTar {
    project.version = version
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

