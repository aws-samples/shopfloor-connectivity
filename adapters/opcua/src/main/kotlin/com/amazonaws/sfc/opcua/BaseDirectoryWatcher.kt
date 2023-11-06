
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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