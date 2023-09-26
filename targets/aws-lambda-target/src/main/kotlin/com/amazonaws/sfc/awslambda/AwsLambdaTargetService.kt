/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awslambda

import com.amazonaws.sfc.awslambda.config.AwsLambdaWriterConfiguration.Companion.AWS_LAMBDA
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

// Lambda target IPC service
class AwsLambdaTargetService : ServiceMain() {
    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, AWS_LAMBDA, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsLambdaTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsLambdaTargetService().run(args)
        }
    }
}
