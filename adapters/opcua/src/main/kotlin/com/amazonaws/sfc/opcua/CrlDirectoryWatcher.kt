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

import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.nio.file.Path
import java.security.cert.X509CRL

internal class CrlDirectoryWatcher(path: Path, scope: CoroutineScope, logger: Logger, onUpdate: () -> Unit) :

        TypedDirectoryWatcher<X509CRL>(path, scope, logger, {
            val ctxLog = logger.getCtxLoggers("CrlDirectoryWatcher", "onUpdate")
            val newEntries = loadCrls(path, logger)
            ctxLog.info("${newEntries.size} certificate revocation lists loaded from directory \"$path\"")
            onUpdate()
            newEntries
        }) {

    init {
        val log = logger.getCtxLoggers("CertificateDirectoryWatcher", "init")
        entries = CertificateHelper.loadAllCRLsInDir(path.toFile())
        log.info("${entries.size} certificate revocation lists loaded from directory \"$path\"")
    }

    companion object {

        private fun loadCrls(path: Path, logger: Logger): Set<X509CRL> {

            val ctxLog = logger.getCtxLoggers("CrlDirectoryWatcher", "loadCrls")
            ctxLog.trace("Loading certificate revocation lists from directory ${path.toAbsolutePath()}")
            val newEntries = CertificateHelper.loadAllCRLsInDir(path.toFile(),
                onFailed = { file, ex ->
                    ctxLog.error("Error loading certificate revocation list from file \"$file\", $ex")
                },
                onLoaded = { file: File, certs: List<X509CRL> ->
                    ctxLog.trace("File \"$file\" ${certs.joinToString(prefix = "\n", separator = "\n") { it.toString() }}")
                })
            return newEntries
        }
    }
}