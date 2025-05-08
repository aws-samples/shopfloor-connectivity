
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking
import org.apache.log4j.LogManager


// S3 target IPC service
class AwsS3TablesTargetService : ServiceMain() {

    init {
        silenceLogger("org.apache.iceberg.aws.shaded.org.apache.http")
        silenceLogger("software.amazon.awssdk.http.apache.internal.conn")
    }
    fun silenceLogger(string: String) {
        val loggerContext = LogManager.getLogger(string)
        loggerContext.level = org.apache.log4j.Level.ERROR
        loggerContext.addAppender(org.apache.log4j.ConsoleAppender())
    }
    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, AWS_S3_TABLES, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsS3TablesTargetWriter.Companion.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsS3TablesTargetService().run(args)
        }
    }
}
