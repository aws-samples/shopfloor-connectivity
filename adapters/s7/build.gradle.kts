import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.time.LocalDate

/*
 *Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

val module = "s7"

val sfcCoreVersion = "1.0.0"
val sfcIpcVersion = "1.0.0"

val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val junitVersion = "5.6.0"
val jvmTarget = "1.8"
val reflectionVersion = "1.6.0"
val commonsCollectionsVersion = "3.1"

val plc4jS7Version = "0.9.1"
val slf4jApiVersion = "2.0.1"
val nettyCodecVersion = "4.1.80.Final"
val fasterXmlVersion = "2.14.2"

plugins {
    id("sfc.kotlin-application-conventions")
    
    java
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$reflectionVersion")
    implementation("commons-collections:commons-collections:$commonsCollectionsVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$fasterXmlVersion")
    implementation("io.netty:netty-codec:$nettyCodecVersion")
    implementation("org.apache.plc4x:plc4j-driver-s7:${plc4jS7Version}")
    implementation("org.slf4j:slf4j-api:${slf4jApiVersion}")
    implementation("org.slf4j:slf4j-nop:${slf4jApiVersion}")
}

application {
    mainClass.set("com.amazonaws.sfc.s7.S7ProtocolService")
    applicationName = project.name
}

tasks.getByName<Zip>("distZip").enabled = false
tasks.distTar {
	project.version = ""
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
