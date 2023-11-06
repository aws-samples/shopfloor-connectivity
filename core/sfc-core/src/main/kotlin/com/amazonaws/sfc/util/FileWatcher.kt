
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import DirectoryWatcher
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.io.path.Path

/**
 * Helper class to detect modifications to a specific file.
 * @property pollInterval Long Polling interval for changes
 * @filename filename String File to watch
 * @param watchEvents WatchEvent Events to watch for
 */
class FileWatcher(
    file: File,
    private val pollInterval: Long = 1000L,
    vararg watchEvents: WatchEvent.Kind<Path> = arrayOf(
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    )
) : Closeable {

    private val watchedFile = Path(file.absolutePath)
    private val watchedDirectory = watchedFile.parent
    private val events = watchEvents
    private var watcher: DirectoryWatcher? = null

    /**
     * Returns changes to the file as a flow. Changes are collected de-duped until a polling interval without changes has occurred.
     */
    val changes = flow {
        watcher = DirectoryWatcher(watchedDirectory.toString(), pollInterval, true, *events)
        watcher!!.changes.filter { it.entry == watchedFile.toString() }.collect { emit(it) }
    }

    override fun close() {
        watcher?.close()
    }
}