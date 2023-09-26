/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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