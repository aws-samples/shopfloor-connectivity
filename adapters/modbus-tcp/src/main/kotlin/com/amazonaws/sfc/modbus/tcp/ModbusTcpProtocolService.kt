/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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

