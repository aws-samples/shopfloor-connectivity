
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awskinesis

import com.amazonaws.sfc.awskinesis.config.AwsKinesisWriterConfiguration.Companion.AWS_KINESIS_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 * IPC service for AWS Kinesis target
 */
class AwsKinesisTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_KINESIS_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsKinesisTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsKinesisTargetService().run(args)
        }
    }
}

