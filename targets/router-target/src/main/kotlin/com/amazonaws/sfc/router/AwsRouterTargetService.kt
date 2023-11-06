
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.router


import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.router.config.RouterWriterConfiguration.Companion.ROUTER
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain

import kotlinx.coroutines.runBlocking


class AwsRouterTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, ROUTER, logger) { configReader, targetID, l, resultHandler ->
            RouterTargetWriter.newInstance(configReader, targetID, l, resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsRouterTargetService().run(args)
        }
    }
}

