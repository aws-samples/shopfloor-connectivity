/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.IOException
import java.nio.file.*
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Data class for returning directory entry events
 * @property kind Kind<out Any> Type of event
 * @property entry String Directory entry created, modified or deleted
 */
data class DirectoryEntryChange(val kind: WatchEvent.Kind<out Any>, val entry: String)

/**
 * Helper class to detect creation and modification of items in a directory
 * @param directory String Directory to watch
 * @param pollInterval Long Polling interval in milliseconds
 * @param fullPath Boolean Return full path names of changed items in changes flow
 * @param watchEvents WatchEvent Events to watch for
 */
class DirectoryWatcher(
    directory: String,
    val pollInterval: Long = 5000,
    private val fullPath: Boolean = true,
    vararg watchEvents: WatchEvent.Kind<*> = arrayOf(
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE)) : Closeable {

    private val watchedDirectory = Path(directory)
    private val watcher = FileSystems.getDefault().newWatchService()
    private val events = watchEvents

    private var closing = false

    /**
     * Flow of changes items. Items are collected and deduplicated until after a polling period in which no changes
     * are occurred.
     */
    val changes = flow {

        if (closing) return@flow

        try {
            withTimeout(1000.toDuration(DurationUnit.SECONDS)) {
                @Suppress("BlockingMethodInNonBlockingContext")
                watchedDirectory.register(watcher, events)
            }
        } catch (ex: IOException) {
            throw Exception("Error registering directory $directory to watcher due to IO error, ${ex.message}")
        } catch (ex: UnsupportedOperationException) {
            throw Exception("Error registering directory $directory to watcher as this is not supported by the file systems watch service, $ex")
        } catch (ex: SecurityException) {
            throw Exception("Error registering directory $directory to watcher because of access restriction , $ex")
        } catch (ex: NotDirectoryException) {
            throw Exception("Error registering directory $directory as this is not a directory , $ex")
        } catch (e: ClosedWatchServiceException) {
            if (!closing) throw e
        } catch (ex: Exception) {
            throw Exception("Error registering directory $directory , $ex")
        }


        try {
            val files = mutableSetOf<DirectoryEntryChange>()
            while (!closing) {
                val key = watcher.poll()
                if (key == null) {
                    if (files.isEmpty()) {
                        delay(pollInterval)
                    } else {
                        // wait with emitting values until there are no new changes to "debounce" updates
                        files.toSet().forEach { emit(it) }
                        files.clear()
                    }
                    continue
                }

                key.pollEvents().forEach { event ->
                    val kind = event.kind()
                    if (kind != StandardWatchEventKinds.OVERFLOW) {
                        var filename = (event.context() as Path).name
                        if (fullPath) {
                            filename = watchedDirectory.resolve(filename).toString()
                        }
                        files.add(DirectoryEntryChange(kind, filename))
                    }
                }
                key.reset()
            }
        } catch (e: ClosedWatchServiceException) {
            if (!closing) throw e
        } finally {
            withTimeout(10.toDuration(DurationUnit.SECONDS)) {
                @Suppress("BlockingMethodInNonBlockingContext")
                watcher.close()
            }
        }
    }.flowOn(Dispatchers.IO)


    override fun close() {
        closing = true
    }


}