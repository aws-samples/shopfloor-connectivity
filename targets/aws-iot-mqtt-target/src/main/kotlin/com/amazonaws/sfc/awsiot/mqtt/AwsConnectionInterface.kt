
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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