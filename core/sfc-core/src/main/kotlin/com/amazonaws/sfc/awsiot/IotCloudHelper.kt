/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.   
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiot

import com.amazonaws.sfc.awsiot.AWSIotException
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.util.BaseRetryableAccessor
import com.amazonaws.sfc.util.CrashableSupplier
import software.amazon.awssdk.http.*
import software.amazon.awssdk.utils.IoUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI


class IotCloudHelper {

    /**
     * Sends Http request to Iot Cloud.
     *
     * @param connManager underlying connection manager to use for sending requests
     * @param thingName   IoT Thing Name
     * @param path        Http url to query
     * @param verb        Http verb for the request
     * @param body        Http body for the request
     * @return Http response corresponding to http request for path
     * @throws AWSIotException when unable to send the request successfully
     */
    fun sendHttpRequest(
        connManager: IotConnectionManager, thingName: String?, path: String,
        verb: String?, body: ByteArray?
    ): IotCloudResponse {
        var uri: URI?
        uri = try {
            connManager.uri
        } catch (e: ConfigurationException) {
            throw AWSIotException(e)
        }
        val innerRequestBuilder = SdkHttpRequest.builder().method(SdkHttpMethod.fromValue(verb))

        if (path.startsWith("https://")) {
            uri = URI.create(path)
            innerRequestBuilder.uri(uri)
        } else {
            innerRequestBuilder.uri(uri).encodedPath(path)
        }
        if (!thingName.isNullOrEmpty()) {
            innerRequestBuilder.appendHeader(HTTP_HEADER_THING_NAME, thingName)
        }
        val request = connManager.client.prepareRequest(
            HttpExecuteRequest.builder()
                .contentStreamProvider(if (body == null) null else ContentStreamProvider { ByteArrayInputStream(body) })
                .request(innerRequestBuilder.build()).build()
        )
        val accessor = BaseRetryableAccessor()
        val getHttpResponse = CrashableSupplier<IotCloudResponse, AWSIotException> { getHttpResponse(request) }
        return accessor.retry(
            RETRY_COUNT, BACKOFF_MILLIS, getHttpResponse,
            HashSet(listOf(AWSIotException::class.java))
        )
    }

    private fun getHttpResponse(request: ExecutableHttpRequest): IotCloudResponse {
        try {
            val httpResponse = request.call()
            httpResponse.responseBody()
                .orElseThrow { AWSIotException("No response body") }
                .use { bodyStream ->
                    return IotCloudResponse(
                        IoUtils.toByteArray(bodyStream),
                        httpResponse.httpResponse().statusCode()
                    )
                }
        } catch (e: IOException) {
            throw AWSIotException("Unable to get response", e)
        }
    }

    companion object {
        private const val HTTP_HEADER_THING_NAME = "x-amzn-iot-thingname"
        private const val RETRY_COUNT = 3
        private const val BACKOFF_MILLIS = 200
    }
}