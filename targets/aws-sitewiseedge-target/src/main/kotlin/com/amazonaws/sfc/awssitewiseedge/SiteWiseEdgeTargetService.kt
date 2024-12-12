
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewiseedge

import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeWriterConfiguration.Companion.AWS_SITEWISEEDGE_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking


class SiteWiseEdgeTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_SITEWISEEDGE_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            SiteWiseEdgeTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            SiteWiseEdgeTargetService().run(args)
        }
    }
}




