
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awslambda

import com.amazonaws.sfc.awslambda.config.AwsLambdaWriterConfiguration.Companion.AWS_LAMBDA
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

// Lambda target IPC service
class AwsLambdaTargetService : ServiceMain() {
    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_LAMBDA, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsLambdaTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsLambdaTargetService().run(args)
        }
    }
}
