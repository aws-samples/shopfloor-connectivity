/*
 *
 *     Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *     Licensed under the Amazon Software License (the "License"). You may not use this  file except in  compliance with the License. A copy of the License is located at :
 *
 *       http://aws.amazon.com/asl/
 *
 *     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

val tag = "git describe --abbrev=0 --tags".runCommand(workingDir = rootDir)
val ref = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
version = tag+"-"+ref

plugins {
    id("java")
}

tasks.register<Zip>("packageDistribution") {
    archiveFileName.set("sfc-$version.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distribution-bundle"))
    from(layout.buildDirectory.dir("distribution"))
}


fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw Exception(error)
        }
        inputStream.bufferedReader().readText().trim()
    }





tasks.named("assemble") {
    finalizedBy("packageDistribution")
}

