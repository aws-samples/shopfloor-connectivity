/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidParameterException
import java.util.*
import kotlin.reflect.full.companionObjectInstance

interface Compressor {
    fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int = 2048, entryName: String = "")
}

interface Compression {
    fun write(buffer: ByteArray, len: Int)
    fun close()
    fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int = 2048)
    val compressionStream: OutputStream
}


object Compress {

    private val compressionTypes = mapOf(
        CompressionType.GZIP to GzipCompression::class,
        CompressionType.ZIP to ZipCompression::class,
        CompressionType.NONE to NoCompression::class
    )

    fun createCompression(compressionType: CompressionType, outputStream: OutputStream, entryName: String): Compression {
        val compressionClass = compressionTypes.getOrDefault(compressionType, NoCompression::class)
        val classConstructor =
            compressionClass.java.constructors.firstOrNull { (it.parameters.size in 1..2) && it.parameters.first().type == OutputStream::class.java }
            ?: throw InvalidParameterException("No valid constructor found to create instance of compression class for compression type ${compressionType.name}")

        return if (classConstructor.parameters.size == 1) classConstructor.newInstance(outputStream) as Compression
        else classConstructor.newInstance(outputStream, entryName) as Compression
    }

    fun compress(compressionType: CompressionType, inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int = 2048, entryName: String) {
        val compressionClass = compressionTypes.getOrDefault(compressionType, NoCompression::class)
        val c = compressionClass.companionObjectInstance as? Compressor
        c?.compress(inputStream, outputStream, readBufferSize, entryName)
    }

    fun compressedDataPayload(compressionType: CompressionType, payload: String, entryName: String): String {
        val compressed = ByteArrayOutputStream()
        compress(compressionType, payload.byteInputStream(), compressed, entryName = entryName)
        return JsonHelper.gsonExtended().toJson(mapOf<String, String>(
            COMPRESSION_ELEMENT to compressionType.name,
            PAYLOAD_ELEMENT to Base64.getEncoder().encodeToString(compressed.toByteArray())))
    }

    const val CONFIG_COMPRESS = "Compression"
    const val CONTENT_TYPE = "ContentType"

    private const val COMPRESSION_ELEMENT = "compression"
    private const val PAYLOAD_ELEMENT = "payload"

}