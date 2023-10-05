/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.storeforward

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.storeforward.FileSystemBufferTargetDirectory.Companion.deserializeTargetDataFile
import com.amazonaws.sfc.storeforward.config.MessageBufferConfiguration
import com.amazonaws.sfc.storeforward.config.StoreForwardTargetConfiguration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration


class FileSystemMessageBuffer(targets: Iterable<String>,
                              private val configuration: StoreForwardTargetConfiguration,
                              private val logger: Logger) : MessageBuffer {

    private val className = this::class.java.simpleName


    private val _targetsDirectories = mutableMapOf(*(targets.mapNotNull { targetID ->
        try {
            val targetPath = File(configuration.directory, validName(targetID))
            if (!targetPath.exists()) {
                logger.getCtxInfoLog(className, "store")("Creating store directory for target \"$targetID\" in \"${configuration.directory?.absolutePath}\"")
                Files.createDirectories(Path(targetPath.absolutePath))
            }
            targetID to FileSystemBufferTargetDirectory(targetPath, configuration.compression)
        } catch (e: Throwable) {
            logger.getCtxErrorLog(className, "store")("Error creating store directory for target \"$targetID\" in \"${configuration.directory?.absolutePath}\", $e")
            null
        }
    }).toTypedArray())

    private fun validName(inputName: String): String {
        return inputName.replace("[^a-zA-Z0-9-_.]".toRegex(), "_")
    }

    override suspend fun add(targetID: String, targetData: TargetData) {
        add(targetID, listOf(targetData))
    }

    override suspend fun add(targetID: String, targetDataList: Iterable<TargetData>) {
        val targetDirectory = _targetsDirectories[targetID] ?: return

        val log = logger.getCtxLoggers(className, "write")

        targetDataList.forEach { targetData ->

            try {

                withTimeout(configuration.writeTimeout) {

                    val targetWithExistingFile = _targetsDirectories.filter { it.key != targetID }.values.firstOrNull { it.find(targetData.serial) != null }

                    return@withTimeout if (targetWithExistingFile == null)
                        targetDirectory.createTargetDataFile(targetData)
                    else
                        targetDirectory.createTargetLink(targetWithExistingFile, targetData)
                }

            } catch (e: Exception) {
                log.error("Error storing target data with serial ${targetData.serial} for target \"$targetID\", $e")
            }
        }
    }


    override suspend fun remove(targetID: String, serial: String) {
        remove(targetID, listOf(serial))
    }

    override suspend fun remove(targetID: String, serials: Iterable<String>) {

        val targetDirectory = _targetsDirectories[targetID] ?: return

        val log = logger.getCtxLoggers(className, "remove")

        serials.forEach { serial ->
            try {
                targetDirectory.remove(serial, configuration.writeTimeout)
            } catch (e: Exception) {
                log.error("Error deleting target data with serial $serial for target \"$targetID\", $e")

            }
        }
    }

    override suspend fun size(targetID: String): Long = _targetsDirectories[targetID]?.size ?: 0L

    override suspend fun count(targetID: String): Int = _targetsDirectories[targetID]?.count ?: 0


    override suspend fun listAfter(targetID: String, bufferConfig: MessageBufferConfiguration): Sequence<TargetData?> = sequence {

        val targetDirectory = _targetsDirectories[targetID] ?: return@sequence

        val logs = logger.getCtxLoggers(className, "list")

        var selectedFiles: List<File>
        runBlocking {
            selectedFiles = targetDirectory.listAfter(configuration.fifo, bufferConfig).toList()
        }

        selectedFiles.filter { it.exists() }.forEach {
            try {
                yield(deserializeTargetDataFile(it))

            } catch (e: Exception) {
                if (it.exists()) {
                    logs.error("Can not deserialize target data from ${it.absolutePath}, $e")
                }
            }
        }
    }

    override suspend fun list(targetID: String, fifo: Boolean): Sequence<TargetData?> = sequence {
        val targetDirectory = _targetsDirectories[targetID]

        val logs = logger.getCtxLoggers(className, "list")

        val files: List<File>?
        runBlocking {
            files = targetDirectory?.list(fifo)?.toList()
        }
        files?.filter { it.exists() }?.forEach {
            try {
                yield(deserializeTargetDataFile(it))

            } catch (e: Exception) {
                if (it.exists()) {
                    logs.error("Can not deserialize target data from ${it.absolutePath}, $e")
                }
            }
        }
    }


    override suspend fun first(targetID: String, retainPeriod: Duration?): TargetData? {
        val targetDirectory = _targetsDirectories[targetID]
        return targetDirectory?.first(configuration)?.let { deserializeTargetDataFile(it) }
    }

    override suspend fun last(targetID: String): TargetData? {
        val targetDirectory = _targetsDirectories[targetID]
        return targetDirectory?.last()?.let { deserializeTargetDataFile(it) }
    }

    override suspend fun clear(targetID: String) {
        _targetsDirectories[targetID]?.clear(configuration.writeTimeout, logger)

    }

    override suspend fun cleanBySize(targetID: String, bufferConfig: MessageBufferConfiguration): Pair<Int, Long> =
        _targetsDirectories[targetID]?.cleanByTargetBufferSize(bufferConfig.retainSize, bufferConfig.writeTimeout, logger) ?: (0 to 0L)


    override suspend fun cleanByAge(targetID: String, bufferConfig: MessageBufferConfiguration): Int =
        _targetsDirectories[targetID]?.cleanByAge(bufferConfig.retainPeriod, bufferConfig.writeTimeout, logger) ?: 0


    override suspend fun cleanByNumber(targetID: String, bufferConfig: MessageBufferConfiguration): Int =
        _targetsDirectories[targetID]?.cleanByNumber(bufferConfig.retainNumber, bufferConfig.writeTimeout, logger) ?: 0


}







