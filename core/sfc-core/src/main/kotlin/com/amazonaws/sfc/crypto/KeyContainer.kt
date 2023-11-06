
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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