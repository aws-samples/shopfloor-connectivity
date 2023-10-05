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

plugins {
    id("sfc.kotlin-library-conventions")
    `maven-publish`
    jacoco
    java
}

group = "com.amazonaws.sfc"
version = "1.0.0"

val awsEncryptionSdkVersion = "2.4.0"
val awsSdkApacheVersion = "2.20.3"
val awsSdkCrtVersion = "0.16.14"
val awsSdkVersion = "2.17.209"

val bouncyCastleVersion = "1.70"

val commonsCliVersion = "1.5.0"
val commonsCodecVersion = "1.15"
val commonsIoVersion = "2.13.0"

val fasterXmlVersion = "2.14.2"
val gsonVersion = "2.9.0"
val jmesPathVersion = "0.5.1"
val log4jVersion = "2.17.2"
val velocityVersion = "2.3"

val junitVersion = "5.6.0"
val mockkVersion = "1.12.0"

val kotlinCoroutinesVersion = "1.6.2"
val kotlinReflectionVersion = "1.6.0"
val kotlinVersion = "1.9.0"

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    api("com.google.code.gson:gson:$gsonVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinReflectionVersion")

    api("commons-cli:commons-cli:$commonsCliVersion")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")

    api("software.amazon.awssdk:secretsmanager:$awsSdkVersion")
    implementation("com.amazonaws:aws-encryption-sdk-java:$awsEncryptionSdkVersion")
    implementation("software.amazon.awssdk.crt:aws-crt:$awsSdkCrtVersion")
    implementation("software.amazon.awssdk:apache-client:$awsSdkApacheVersion")
    implementation("software.amazon.awssdk:auth:$awsSdkVersion")

    implementation("software.amazon.awssdk:aws-core:$awsSdkVersion")

    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")

    api("io.burt:jmespath-core:$jmesPathVersion")
    api("org.apache.velocity:velocity-engine-core:$velocityVersion")

    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$fasterXmlVersion")

    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

publishing {

    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = group as String
            artifactId = "sfc-core"
            version = version
        }
    }

}

tasks.build {
    finalizedBy(tasks.publish)
}

tasks {
    //    jacocoTestCoverageVerification {
    //        violationRules {
    //            rule {
    //                limit {
    //                    counter = "LINE"
    //                    minimum = BigDecimal.valueOf(0.8)
    //                }
    //                limit {
    //                    counter = "BRANCH"
    //                    minimum = BigDecimal.valueOf(0.6)
    //                }
    //            }
    //        }
    //    }
    //    check {
    //        dependsOn(jacocoTestCoverageVerification)
    //    }

    withType<JacocoReport> {
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it).apply {
                            exclude(
                                "**/Generated*.class",

                                )
                        }
                    }
                )
            )
        }
    }
}

