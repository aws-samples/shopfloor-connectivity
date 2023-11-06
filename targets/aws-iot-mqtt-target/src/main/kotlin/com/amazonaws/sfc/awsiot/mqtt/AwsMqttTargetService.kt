
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.mqtt

import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotWriterConfiguration.Companion.AWS_IOT_MQTT_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 *  IPC service for AWS IoT core target
 */
class AwsMqttTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_IOT_MQTT_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsIotMqttTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsMqttTargetService().run(args)
        }
    }
}




