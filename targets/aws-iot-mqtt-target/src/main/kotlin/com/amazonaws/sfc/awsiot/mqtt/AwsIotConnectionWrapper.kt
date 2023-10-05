/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiot.mqtt

import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotTargetConfiguration
import com.amazonaws.sfc.util.SfcException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import software.amazon.awssdk.crt.io.ClientBootstrap
import software.amazon.awssdk.crt.io.EventLoopGroup
import software.amazon.awssdk.crt.io.HostResolver
import software.amazon.awssdk.crt.mqtt.MqttClientConnection
import software.amazon.awssdk.crt.mqtt.MqttMessage
import software.amazon.awssdk.crt.mqtt.QualityOfService
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder
import java.util.concurrent.TimeUnit

class AwsIotConnectorException(message: String) : SfcException(message)

/**
 * Class to abstract actual MQTT client to MqttClient interface for mock testing.
 * @see <a href="http://aws-iot-device-sdk-java-docs.s3-website-us-east-1.amazonaws.com/com/amazonaws/services/iot/client/AWSIotMqttClient.html">AWSIotMqttClient</a>
 **/
class AwsIotConnectionWrapper
private constructor(private val client: MqttClientConnection, private val config: AwsIotTargetConfiguration) : AwsConnectionInterface {

    private val connectMutex = Mutex()

    /**
     * Publishes a message.
     * @param topic Topic name
     * @param payload Message eto publish
     */
    override fun publish(topic: String, payload: String, qos: QualityOfService) {
        try {
            client.publish(MqttMessage(topic, payload.encodeToByteArray(), qos)).get(config.publishTimeout, TimeUnit.MILLISECONDS)
        } catch (e: Throwable) {
            throw AwsIotConnectorException("Error publishing message, $e")
        }
    }

    /**
     * Connect to the MQTT broker.
     */
    override fun connect() {

        runBlocking {
            connectMutex.withLock {

                try {
                    client.connect().get(config.connectTimeout, TimeUnit.MILLISECONDS)
                } catch (e: Throwable) {
                    throw AwsIotConnectorException("Can not connect, $e")
                }
            }
        }
    }

    /**
     * Closes the client.
     */
    override fun close() {
        try {
            client.disconnect()
        } catch (_: Throwable) {
            // No action required as it part of cleanup when process is shut down
        }
    }

    companion object {
        /**
         * Creates an instance of a MQTT client
         * @param mqttConfig Mqtt target broker configuration
         * @see AwsIotTargetConfiguration
         * @param clientID String Client ID to use for connection
         * @return AwsIotConnectionWrapper Wrapped client to communicate with the broker
         */
        fun newConnection(mqttConfig: AwsIotTargetConfiguration, clientID: String): AwsIotConnectionWrapper {

            val eventLoopGroup = EventLoopGroup(1)
            val resolver = HostResolver(eventLoopGroup)
            val builder = AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(mqttConfig.certificate, mqttConfig.key)
                .withBootstrap(ClientBootstrap(eventLoopGroup, resolver))
                .withCertificateAuthorityFromPath(null, mqttConfig.rootCA)
                .withClientId(clientID)
                .withCleanSession(true)
                .withEndpoint(mqttConfig.endpoint)

            val connection: MqttClientConnection = builder.build()

            return AwsIotConnectionWrapper(connection, mqttConfig)

        }
    }

}