
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot


import com.amazonaws.sfc.util.ClientConfigurationUtils
import software.amazon.awssdk.http.SdkHttpClient
import java.io.Closeable
import java.net.URI

class IotConnectionManager(private val clientConfiguration: AwsIotCredentialProviderClientConfiguration) : Closeable {

    private var _client: SdkHttpClient? = null

    val client: SdkHttpClient by lazy {
        _client = initConnectionManager()
        _client!!
    }


    /**
     * Get URI for connecting to AWS IoT.
     * @return URI to AWS IoT, based on device configuration
     */
    val uri: URI
        get() {
            return URI.create("https://" + clientConfiguration.iotCredentialEndpoint)
        }

    private fun initConnectionManager(): SdkHttpClient {
        return ClientConfigurationUtils.getConfiguredClientBuilder(clientConfiguration).build()
    }

    /**
     * Clean up underlying connections and close gracefully.
     */
    @Synchronized
    override fun close() {
        _client?.close()
    }

}