/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */


import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


group = "com.amazonaws.sfc"
version = "1.0.0"

val protobufVersion = "3.21.2"
val grpcKotlinVersion = "0.1.1"
val grpcVersion = "1.54.1"
val reflectionVersion = "1.6.0"
val kotlinAnnotationVersion = "1.3.2"
val commonsCliVersion = "1.5.0"
val jvmTarget = "1.8"

val sfcCoreVersion = "1.0.0"


val junitVersion = "5.6.0"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinReflectionVersion = "1.6.0"
val kotlinVersion = "1.9.0"

repositories {
    mavenCentral()
    mavenLocal {
        url = uri("file://tmp/repo")
    }
}

plugins {
    id("com.google.protobuf") version "0.8.18"
    kotlin("jvm") version "1.8.10"
    `java-library`
    idea
    `maven-publish`
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    implementation("com.amazonaws.sfc:sfc-core:$sfcCoreVersion")

    implementation("commons-cli:commons-cli:$commonsCliVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    api("com.google.protobuf:protobuf-java:$protobufVersion")
    api("com.google.protobuf:protobuf-java-util:$protobufVersion")

    api("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    api("io.grpc:grpc-netty-shaded:$grpcVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")

    implementation("org.jetbrains.kotlin:kotlin-reflect:$reflectionVersion")

    // Java
    compileOnly("javax.annotation:javax.annotation-api:$kotlinAnnotationVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain(8)
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmTarget
        freeCompilerArgs = listOf("-opt-in=kotlin.time.ExperimentalTime", "-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmTarget
        freeCompilerArgs = listOf("-opt-in=kotlin.time.ExperimentalTime", "-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        // Specify protoc to generate using kotlin protobuf plugin
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }

        // Specify protoc to generate using our grpc kotlin plugin
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                // Generate Java gRPC classes
                id("grpc")
                // Generate Kotlin gRPC using the custom plugin from library
                id("grpckt")
            }
        }
    }
}



idea {
    module {
        generatedSourceDirs.addAll(listOf(
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/java")
        ))
    }
}

publishing {

    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = group as String
            artifactId = "sfc-ipc"
            version = version
        }
    }

    repositories {
        maven {
            url = uri("file://tmp/repo")
        }
    }

}


tasks.compileJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.compileTestJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.build {
    finalizedBy(tasks.publish)
}

tasks.processResources {
    dependsOn("extractProto")
}







