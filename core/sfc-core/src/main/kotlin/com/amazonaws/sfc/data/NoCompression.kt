
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import java.io.InputStream
import java.io.OutputStream

class NoCompression(override val compressionStream: OutputStream) : Compression {

    override fun write(buffer: ByteArray, len: Int) {
        compressionStream.write(buffer, 0, len)
    }

    override fun close() {
        compressionStream.flush()
        compressionStream.close()
    }

    override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int) {
        NoCompression.compress(inputStream, outputStream, readBufferSize)
    }

    companion object : Compressor {
        override fun compress(inputStream: InputStream, outputStream: OutputStream, readBufferSize: Int, entryName: String) {
            val noCompression = NoCompression(outputStream)
            val buffer = ByteArray(readBufferSize)
            var len = inputStream.read(buffer)
            while (len >= 0) {
                noCompression.write(buffer, len)
                len = inputStream.read(buffer)
            }
            noCompression.close()
        }

    }
}