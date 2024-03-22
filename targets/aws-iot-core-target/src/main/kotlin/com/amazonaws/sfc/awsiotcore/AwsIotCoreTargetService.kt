
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiotcore

import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreWriterConfiguration.Companion.AWS_IOT_CORE_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


/**
 * AWS IoT core IPC service.
 * @see Logger
 */
class AwsIotCoreTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, AWS_IOT_CORE_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsIotCoreTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsIotCoreTargetService().run(args)
        }
    }
}


