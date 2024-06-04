// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.util

import com.amazonaws.sfc.config.IncludeResolver.IncludeResolverException
import kotlinx.coroutines.runBlocking
import java.io.File


class FileReaderCache {

    class FileCacheException( message : String) : Exception(message)


    fun remove(file : File) {
        return cache.remove(file.absolutePath)
    }

    operator fun get(file : File) : String?{
        return runBlocking {
            cache.getItemAsync(file.absolutePath).await()
        }
    }

    private val cache = LookupCacheHandler<String, String, Nothing>(
        supplier = { file ->
            val f = File(file)
            if (!f.exists() || !f.canRead())
                throw IncludeResolverException("File ${f.absolutePath} does not exists or can nor be read")
            f.readText()
        },
    )
}


