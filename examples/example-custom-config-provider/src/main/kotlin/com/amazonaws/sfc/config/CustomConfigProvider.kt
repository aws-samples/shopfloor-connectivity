/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.PublicKey

@ConfigurationClass
class CustomConfigProvider(private val configStr: String, private val configVerificationKey: PublicKey?, private val logger: Logger) : ConfigProvider {

    private val ch = Channel<String>(1)
    private val scope = buildScope("CustomConfigProvider")

    // this code could for example call out to external sources and combine retrieved information with
    // data from the passed in configuration
    val worker = scope.launch {
        val errorLog = logger.getCtxErrorLog(this::class.java.name, "worker")
        while (true) {
            if (configVerificationKey != null) {
                if (!ConfigVerification.verify(configStr, configVerificationKey)) {
                    errorLog("Content of configuration could not be verified")
                    continue
                }
            }
            // simulate building a new config every minute
            ch.send(configStr)
            delay(60000)
        }
    }


    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return CustomConfigProvider(createParameters[0] as String, createParameters[1] as PublicKey?, createParameters[2] as Logger)
        }

    }

}