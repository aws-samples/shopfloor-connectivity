
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.http

import com.amazonaws.sfc.awsiot.http.config.AwsIotHttpWriterConfiguration.Companion.AWS_IOT_HTTP_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


/**
 * AWS IoT core IPC service.
 * @see Logger
 */
class AwsIotHttpTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_IOT_HTTP_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsIotHttpTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsIotHttpTargetService().run(args)
        }
    }
}


