
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
        try {
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
        }catch (e :  Exception){
            logger.getCtxErrorLog(this::class.java.name, "worker")("Error in worker thread")
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