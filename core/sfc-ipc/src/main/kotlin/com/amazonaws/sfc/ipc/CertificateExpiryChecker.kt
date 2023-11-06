
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.system.DateTime.add
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.time.Period
import java.time.temporal.ChronoUnit

class CertificateExpiryChecker(private val certificateFiles: List<File>, private val expirationWarningPeriodDays: Int, logger: Logger) {

    private val className = this::class.simpleName.toString()

    private val scope = buildScope(className)


    val certificates = certificateFiles.flatMap { f ->
        try {
            CertificateHelper.loadX509Certificates(f)
        } catch (e: Exception) {
            logger.getCtxErrorLog(className, "certificates")("Error loading certificate file ${f.absolutePath}")
            emptyList()
        }
    }

    private val job = if (certificates.isEmpty() || expirationWarningPeriodDays <= 0) null else
        scope.launch("GRPC Service CertificateExpiryWatcher", Dispatchers.IO) {

            val ctxLog = logger.getCtxLoggers(className, "Check Certificate Expiration")

            while (true) {

                val now = DateTime.systemDateUTC()

                certificates.forEach { certificate ->

                    val certificateName = certificate.subjectX500Principal.name


                    if (certificate.notAfter <= now.add(Period.ofDays(expirationWarningPeriodDays * -1))) {

                        if (certificate.notAfter <= now) {
                            ctxLog.error("Certificate $certificateName expired at ${certificate.notAfter}")
                        } else {
                            val daysBetween = ChronoUnit.DAYS.between(certificate.notAfter.toInstant(), now.toInstant())
                            ctxLog.warning("Certificate $certificateName will expire in $daysBetween days at ${certificate.notAfter}")
                        }

                    } else {
                        ctxLog.info("Checking expiration of certificate $certificateName, valid until ${certificate.notAfter}")
                    }
                }
                DateTime.delayUntilNextMidnightUTC()
            }
        }

    fun stop() {
        job?.cancel()
    }

}