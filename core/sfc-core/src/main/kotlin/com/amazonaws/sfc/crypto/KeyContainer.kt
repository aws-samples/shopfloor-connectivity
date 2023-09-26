/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.crypto

import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths

sealed class KeyContainer {

    abstract val scheme: String
    abstract val keyBytes: ByteArray

    fun uriFromPossibleFileURIString(path: String): URI {
        return try {
            var u = URI(path)
            if (u.scheme.isNullOrEmpty()) {
                u = URI(SecurityService.DefaultCryptoKeyProvider.KEY_TYPE_FILE, path, null as String?)
            }
            u
        } catch (e: URISyntaxException) {
            val p = Paths.get(path)
            if (!Files.exists(p, *arrayOfNulls(0))) {
                throw CryptoException("Can't parse path string as URI and no file exists at the path")
            }
            p.toUri()
        }
    }
}