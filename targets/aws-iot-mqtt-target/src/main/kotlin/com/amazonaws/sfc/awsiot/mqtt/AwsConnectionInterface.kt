/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiot.mqtt

import software.amazon.awssdk.crt.mqtt.QualityOfService

/** AWS IoT Core client interface
 *  @see <a href="http://aws-iot-device-sdk-java-docs.s3-website-us-east-1.amazonaws.com/com/amazonaws/services/iot/client/AWSIotMqttClient.html">AWSIotMqttClient</a>
 **/
interface AwsConnectionInterface {
    fun publish(topic: String, payload: String, qos: QualityOfService)
    fun connect()
    fun close()
}