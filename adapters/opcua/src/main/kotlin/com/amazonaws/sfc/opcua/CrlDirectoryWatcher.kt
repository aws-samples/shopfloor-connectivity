
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
                    ctxLog.errorEx("Error loading certificate revocation list from file \"$file\"", ex)
                },
                onLoaded = { file: File, certs: List<X509CRL> ->
                    ctxLog.trace("File \"$file\" ${certs.joinToString(prefix = "\n", separator = "\n") { it.toString() }}")
                })
            return newEntries
        }
    }
}