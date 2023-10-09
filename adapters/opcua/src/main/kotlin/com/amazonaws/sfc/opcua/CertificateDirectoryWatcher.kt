/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.security.cert.X509Certificate

internal class CertificateDirectoryWatcher(path: Path, scope: CoroutineScope, private val logger: Logger, onUpdate: (Path) -> Unit) :

        TypedDirectoryWatcher<X509Certificate>(path, scope, logger, {
            val ctxLog = logger.getCtxLoggers("CertificateDirectoryWatcher", "onUpdate")
            ctxLog.trace("Loading certificates from directory ${path.toAbsolutePath()}")
            val newEntries = loadCertificates(path, logger)
            ctxLog.info("${newEntries.size} certificates loaded from directory \"$path\"")
            onUpdate(path)
            newEntries
        }) {

    init {
        val log = logger.getCtxLoggers("CertificateDirectoryWatcher", "init")
        entries = loadCertificates(path, logger)
        log.info("${entries.size} certificates loaded from directory \"$path\"")
    }

    companion object {
        private fun loadCertificates(path: Path, logger: Logger): Set<X509Certificate> {
            val ctxLog = logger.getCtxLoggers("CertificateDirectoryWatcher", "loadCertificates")

            return CertificateHelper.loadAllCertificatesInDir(path.toFile(),
                onFailed = { f: File, ex: Exception ->
                    ctxLog.error("Error loading certificate from file \"$f\", $ex")
                },
                onLoaded = { f: File, certs: List<X509Certificate> ->
                    ctxLog.trace("File \"$f\" ${certs.joinToString(prefix = "\n", separator = "\n") { it.toString() }}")
                })

        }
    }

}