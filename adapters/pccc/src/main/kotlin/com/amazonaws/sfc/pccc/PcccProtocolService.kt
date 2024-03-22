
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.pccc

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.ipc.IpcAdapterService
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


/**
 * OPC UA protocol IPC service
 * @constructor
 */
class PcccProtocolService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return IpcAdapterService.createProtocolAdapterService(
            args,
            configuration,
            logger
        ) { _adapterID: String, _configReader: ConfigReader, _logger: Logger ->
            PcccAdapter.createPcccAdapter(_adapterID, _configReader, _logger)

        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>) = runBlocking {
            PcccProtocolService().run(args)
        }
    }
}