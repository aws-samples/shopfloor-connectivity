
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsfirehose

import com.amazonaws.sfc.awsfirehose.config.AwsFirehoseWriterConfiguration.Companion.AWS_KINESIS_FIREHOSE
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 * IPC service for AWS Kinesis firehose target
 */
class AwsKinesisFirehoseTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, AWS_KINESIS_FIREHOSE, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsFirehoseTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsKinesisFirehoseTargetService().run(args)
        }
    }
}
