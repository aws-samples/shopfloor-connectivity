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

