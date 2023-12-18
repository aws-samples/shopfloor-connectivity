// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.csvfile

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.ipc.IpcAdapterService
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


/**
 * CSV-FILE adapter IPC service
 * @constructor
 */
class CsvFileService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return IpcAdapterService.createProtocolAdapterService(args, configuration, logger) { _adapterID, _configReader: ConfigReader, _logger: Logger ->
            CsvFileAdapter.createFileAdapter(_adapterID, _configReader, _logger)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>) = runBlocking {
            CsvFileService().run(args)
        }
    }
}