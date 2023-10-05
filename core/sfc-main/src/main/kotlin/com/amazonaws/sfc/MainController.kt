/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
