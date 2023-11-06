
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.ipc.WriteValuesRequest
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueAsNativeExt.asTargetData
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.storeforward.config.MessageBufferConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.SfcException
import com.amazonaws.sfc.util.constrainFilePermissions
import com.google.protobuf.CodedOutputStream
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal data class FileSystemBufferTargetDirectory(val directory: File, val compression: Boolean) {

    private var hardLinkNotSupported = false

    private val files: List<File>
        get() {
            return directory.listFiles()?.toList()?.filter { it.isFile && (!it.name.startsWith(".")) } ?: emptyList()
        }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    fun File.compareTo(other: File): Int {
        val a = this.lastModified
        val b = other.lastModified
        return when {
            a > b -> 1
            a < b -> -1
            else -> 0
        }
    }

    private val sortedFiles: List<File>
        get() {
            return try {
                files.sorted()
            } catch (_: Exception) {
                emptyList()
            }
        }


    suspend fun first(config: MessageBufferConfiguration): File? {
        if (files.isEmpty()) return null
        return listAfter(true, config).first { it.exists() }
    }

    fun last(): File? {
        if (files.isEmpty()) return null
        val f = files.maxByOrNull {
            it.lastModified
        }
        return if (f?.exists() == true) f else last()
    }

    val size: Long
        get() {
            return files.sumOf {
                it.length
            }
        }

    val count: Int
        get() {
            return files.toList().size
        }

    suspend fun list(fifo: Boolean): Sequence<File> = sequence {

        if (fifo) {
            files.asSequence().filter {
                it.exists()
            }.toList().forEach { yield(it) }
        } else {
            files.asSequence().filter {
                it.exists()
            }.toList().reversed().forEach { yield(it) }
        }

    }

    private fun filesOutsideOfRetentionNumber(retainNumber: Int): List<File> {
        val tooMany = maxOf((files.size - retainNumber), 0)
        return if (tooMany > 0) {
            sortedFiles.take(tooMany).toList()
        } else emptyList()
    }

    private fun filesOutSideSizeRetention(retainSize: Long): List<File> {
        var size = files.sumOf {
            it.length
        }

        return sortedFiles.takeWhile {
            val largerThanMaxSize = (size > retainSize)
            if (largerThanMaxSize) {
                size -= it.length
            }
            largerThanMaxSize
        }.toList()
    }

    fun find(serial: String): File? = if (File(directory, serial).exists()) File(directory, serial) else null

    private fun filesOutSideRetentionPeriod(retainPeriod: Duration): List<File> {
        val now = DateTime.systemDateTimeUTC()
        val skipBefore = now.minusMillis(retainPeriod.inWholeMilliseconds)
        return (sortedFiles.takeWhile {
            (Instant.ofEpochMilli(it.lastModified).isBefore(skipBefore))
        }.toList())
    }

    fun createTargetDataFile(targetData: TargetData): File? {
        return try {
            serializeTargetDataToFile(targetData, File(directory, targetData.serial))
        } catch (_: FileAlreadyExistsException) {
            null
        } catch (e: Exception) {
            throw SfcException("Can not create file ${targetData.serial} in directory ${directory.absolutePath}, $e")
        }
    }


    fun createTargetLink(sourceDirectory: FileSystemBufferTargetDirectory,
                         targetData: TargetData): File? {
        val existingFile = Path(sourceDirectory.directory.absolutePath, targetData.serial)
        return try {
            val link = Files.createLink(Path(directory.absolutePath, targetData.serial), existingFile)
            link.toFile()
        } catch (_: FileAlreadyExistsException) {
            null
        } catch (_: UnsupportedOperationException) {
            hardLinkNotSupported = true
            createTargetDataFile(targetData)
        } catch (e: Exception) {
            createTargetDataFile(targetData)
        }
    }

    suspend fun remove(serial: String, writeTimeout: Duration) {

        withTimeout(writeTimeout) {
            val file = File(directory, serial)
            if (file.exists()) {
                file.delete()
            }

        }
    }

    suspend fun cleanByTargetBufferSize(retainSize: Long?, writeTimeout: Duration, logger: Logger): Pair<Int, Long> {

        if (retainSize == null) return 0 to 0L

        var size = size
        var deletedSize = 0L

        val toDelete = filesOutSideSizeRetention(retainSize).takeWhile { size > retainSize }.map {
            size -= it.length
            deletedSize += it.length
            it
        }.toList()

        toDelete.forEach { f ->
            try {
                remove(f.name, writeTimeout)
            } catch (e: IOException) {
                logger.getCtxErrorLog("FileSystemMessageBuffer.FileSystemBufferTargetDirectory", "cleanByTargetBufferSize")("Error deleting file ${f.absolutePath} for , $e")
            }
        }

        return toDelete.size to deletedSize
    }


    suspend fun cleanByAge(retainPeriod: Duration?, writeTimeout: Duration, logger: Logger): Int {

        if (retainPeriod == null) return 0

        val retainDuration = retainPeriod.plus(retainPeriodRemoveMargin)
        val toDelete = filesOutSideRetentionPeriod(retainDuration).toList()

        toDelete.forEach { f ->
            try {
                remove(f.name, writeTimeout)
            } catch (e: IOException) {
                logger.getCtxErrorLog("FileSystemMessageBuffer.FileSystemBufferTargetDirectory", "cleanByFileAge")("Error deleting file ${f.absolutePath} for , $e")
            }
        }
        return toDelete.size
    }


    suspend fun cleanByNumber(retainNumber: Int?, writeTimeout: Duration, logger: Logger): Int {

        if (retainNumber == null) return 0

        val toDelete = filesOutsideOfRetentionNumber(retainNumber).toList()

        toDelete.forEach { f ->
            try {
                remove(f.name, writeTimeout)
            } catch (e: IOException) {
                logger.getCtxErrorLog("FileSystemMessageBuffer.FileSystemBufferTargetDirectory", "cleanByNumberOfFiles")("Error deleting file ${f.absolutePath} for , $e")
            }
        }
        return toDelete.size
    }

    suspend fun clear(writeTimeout: Duration, logger: Logger) {
        files.forEach {
            try {
                remove(it.name, writeTimeout)
            } catch (e: IOException) {
                logger.getCtxErrorLog("FileSystemMessageBuffer.FileSystemBufferTargetDirectory", "clean")("Error deleting file ${it.absolutePath} for , $e")
            }
        }
    }

    suspend fun listAfter(fifo: Boolean, bufferConfig: MessageBufferConfiguration) = sequence {


        val skipped: Int = when {

            bufferConfig.retainNumber != null -> filesOutsideOfRetentionNumber(bufferConfig.retainNumber!!).toList().size

            bufferConfig.retainPeriod != null -> {
                filesOutSideRetentionPeriod(bufferConfig.retainPeriod!!).toList().size
            }

            bufferConfig.retainSize != null -> {
                filesOutSideSizeRetention(bufferConfig.retainSize!!).toList().size
            }

            else -> 0
        }

        if (fifo) {
            files.asSequence().drop(skipped).filter {
                it.exists()
            }.toList().forEach { yield(it) }
        } else {
            files.asSequence().drop(skipped).filter {
                it.exists()
            }.toList().reversed().forEach { yield(it) }
        }
    }

    companion object {

        private fun serializeTargetDataToFile(targetData: TargetData, file: File): File {
            val targetRequest = GrpcTargetValueFromNativeExt.newWriteValuesRequest(targetData, false)
            val stream = FileOutputStream(file.absolutePath)
            val codedStream = CodedOutputStream.newInstance(stream)
            targetRequest.writeTo(codedStream)
            codedStream.flush()
            stream.close()
            constrainFilePermissions(file)
            return file
        }

        fun deserializeTargetDataFile(file: File): TargetData {

            val serialized = file.readBytes()
            val deserialized = WriteValuesRequest.newBuilder().mergeFrom(serialized).build()
            return deserialized.asTargetData()

        }

        val File.lastModified: Long
            get() {
                return try {
                    this.lastModified()
                } catch (_: Exception) {
                    0L
                }
            }

        val File.length: Long
            get() {
                return try {
                    this.length()
                } catch (_: Exception) {
                    0L
                }
            }

        val retainPeriodRemoveMargin = 2.toDuration(DurationUnit.MINUTES)


    }
}