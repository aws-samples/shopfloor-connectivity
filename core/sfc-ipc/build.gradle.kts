// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.amazonaws.sfc"
version = rootProject.extra.get("sfc_release")!!

val protobufVersion = "3.21.7"
val grpcKotlinVersion = "1.3.0"
val grpcVersion = "1.54.1"
val reflectionVersion = "1.6.0"
val kotlinAnnotationVersion = "1.3.2"
val commonsCliVersion = "1.5.0"
val sfcCoreVersion = version
val kotlinCoroutinesVersion = "1.6.2"
val kotlinReflectionVersion = "1.6.0"


plugins {
    id("com.google.protobuf") version "0.8.18"
    id("sfc.kotlin-library-conventions")
    idea
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation(project(":core:sfc-core"))
    implementation("commons-cli:commons-cli:$commonsCliVersion")
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
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
//            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion"
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

}

tasks.build {
    finalizedBy(tasks.publish)
}

tasks.processResources {
    dependsOn("extractProto")
}

