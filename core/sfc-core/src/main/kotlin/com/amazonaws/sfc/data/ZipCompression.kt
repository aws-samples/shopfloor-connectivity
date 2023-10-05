/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipCompression(outputStream: OutputStream, private val entryName: String = "") : Compression {

    override val compressionStream by lazy {
        val s = ZipOutputStream(outputStream)
        val e = ZipEntry(entryName)
        s.putNextEntry(e)
        s
    }

    override fun write(buffer: ByteArray, len: Int) {
        compressionStream.write(buffer, 0, len)
    }

    override fun close() {
        compressionStream.flush()
        compressionStream.closeEntry()
        compressionStream.close()
    }

    override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int) {
        ZipCompression.compress(inputStream, outputStream, readBufferSize)
    }


    companion object : Compressor {
        override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int, entryName: String) {
            val zip = ZipCompression(outputStream, entryName)
            val buffer = ByteArray(readBufferSize)
            var len = inputStream.read(buffer)
            while (len >= 0) {
                zip.write(buffer, len)
                len = inputStream.read(buffer)
            }
            zip.close()
        }

        fun uncompress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int = 1024) {
            val s = ZipInputStream(inputStream)
            s.nextEntry
            val buffer = ByteArray(readBufferSize)
            var n = s.read(buffer, 0, readBufferSize)
            while (n != -1) {
                outputStream.write(buffer, 0, n)
                n = s.read(buffer, 0, readBufferSize)
            }
        }
    }
}