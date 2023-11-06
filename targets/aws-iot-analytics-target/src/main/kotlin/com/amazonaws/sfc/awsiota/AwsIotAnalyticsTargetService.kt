
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiota

import com.amazonaws.sfc.awsiota.config.AwsIotAnalyticsWriterConfiguration.Companion.AWS_IOT_ANALYTICS
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 * AWS Iot Analytics target IPC service
 */
class AwsIotAnalyticsTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_IOT_ANALYTICS, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsIotAnalyticsTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsIotAnalyticsTargetService().run(args)
        }
    }
}

