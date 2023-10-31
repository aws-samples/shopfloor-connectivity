/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

}
