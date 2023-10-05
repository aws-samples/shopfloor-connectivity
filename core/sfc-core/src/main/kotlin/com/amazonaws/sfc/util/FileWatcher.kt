/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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