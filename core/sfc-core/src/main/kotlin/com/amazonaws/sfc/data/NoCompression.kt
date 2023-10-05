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