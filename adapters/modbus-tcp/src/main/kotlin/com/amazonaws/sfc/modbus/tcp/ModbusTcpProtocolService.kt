
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp

import com.amazonaws.sfc.ipc.IpcAdapterService.Companion.createProtocolAdapterService
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.modbus.tcp.ModbusTcpAdapter.Companion.createModbusTcpAdapter
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


/**
 * Modbus TCP IPC service
 */

class ModbusTcpProtocolService : ServiceMain() {
    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createProtocolAdapterService(args, configuration, logger) { _adapterID: String, _configReader, _logger ->
            createModbusTcpAdapter(_adapterID, _configReader, _logger)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")

        fun main(args: Array<String>) = runBlocking {
            ModbusTcpProtocolService().run(args)

        }
    }
}

