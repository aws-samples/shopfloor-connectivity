/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
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