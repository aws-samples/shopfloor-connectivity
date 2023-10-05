/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

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
class OpcuaProtocolService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return IpcAdapterService.createProtocolAdapterService(args, configuration, logger) { a: String, c: ConfigReader, l: Logger ->
            OpcuaAdapter.createOpcuaAdapter(adapterID = a, configReader = c, logger = l)

        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>) = runBlocking {
            OpcuaProtocolService().run(args)
        }
    }
}