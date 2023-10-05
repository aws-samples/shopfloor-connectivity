/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

import DirectoryEntryChange
import DirectoryWatcher
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.nio.file.Path

internal abstract class TypedDirectoryWatcher<T>(
    private val watchedDirectory: Path,
    scope: CoroutineScope,
    logger: Logger,
    onUpdate: () -> Set<T>
) : Closeable {

    private val className = this::class.java.simpleName.toString()

    private var watcher: DirectoryWatcher? = null


    @OptIn(FlowPreview::class)
    private val watchJob = scope.launch {
        watcher = DirectoryWatcher(watchedDirectory.toAbsolutePath().toString())
        watcher?.changes?.debounce(watcher!!.pollInterval)?.collect { _: DirectoryEntryChange ->
            val info = logger.getCtxInfoLog(className, "DirectoryEntryChange")
            info("File changes detected in directory $watchedDirectory")
            entries = onUpdate()
        }
    }

    override fun close() {
        watcher?.close()
        watchJob.cancel()
    }

    var entries: Set<T> = setOf()
}