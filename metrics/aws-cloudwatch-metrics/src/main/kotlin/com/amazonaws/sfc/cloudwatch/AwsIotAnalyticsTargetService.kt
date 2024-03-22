
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch


import com.amazonaws.sfc.ipc.IpcMetricsServer.Companion.createIpcMetricsServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 * AWS Cloudwatch metrics A IPC service
 */
class AwsCloudWatchMetricsWriterService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcMetricsServer(args, configuration, logger) { _configReader, _logger ->
            AwsCloudWatchMetricsWriter.newInstance(_configReader, _logger)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsCloudWatchMetricsWriterService().run(args)
        }
    }
}

