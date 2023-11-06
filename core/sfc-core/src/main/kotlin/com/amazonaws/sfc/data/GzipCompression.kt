
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

class GzipCompression(outputStream: OutputStream, val entryName: String) : Compression {

    override val compressionStream by lazy {
        GZIPOutputStream(outputStream)
    }

    override fun write(buffer: ByteArray, len: Int) {
        compressionStream.write(buffer, 0, len)
    }

    override fun close() {
        compressionStream.flush()
        compressionStream.close()
    }

    override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int) {
        compress(inputStream, outputStream, readBufferSize, entryName)
    }

    companion object : Compressor {
        override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int, entryName: String) {
            val zip = GzipCompression(outputStream, entryName)
            val buffer = ByteArray(readBufferSize)
            var len = inputStream.read(buffer)
            while (len >= 0) {
                zip.write(buffer, len)
                len = inputStream.read(buffer)
            }
            zip.close()
        }

    }
}