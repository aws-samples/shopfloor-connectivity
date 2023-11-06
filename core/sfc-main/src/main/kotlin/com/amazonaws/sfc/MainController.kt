
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Main class for running the SFC core controller
 * @constructor
 */
class MainController : ServiceMain() {


    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return MainControllerService.createController(args, configuration, logger)
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            LoggerFactory.getLogger("")

            MainController().run(args)

        }
    }


}
