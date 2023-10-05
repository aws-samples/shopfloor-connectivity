/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.s7

import org.apache.plc4x.java.api.PlcConnection
import org.apache.plc4x.java.api.exceptions.PlcConnectionException
import org.apache.plc4x.java.spi.configuration.ConfigurationFactory
import org.apache.plc4x.java.spi.connection.DefaultNettyPlcConnection
import org.apache.plc4x.java.spi.connection.GeneratedDriverBase
import org.apache.plc4x.java.spi.generation.Message
import org.apache.plc4x.java.transport.tcp.TcpTransport
import java.lang.Boolean.parseBoolean
import java.util.regex.Pattern

// Extends PLC4J class to overload the getConnection method. For loading of classes PLC4J requires
// all jars to be in the class path loaded by the Thread classloader, where SFC uses a pure config
// approach loading the configured classes using the URLClassLoader.
internal abstract class CustomDriverBase<B : Message?> : GeneratedDriverBase<B>() {

    override fun getConnection(connectionString: String?): PlcConnection {

        val matcher = URI_PATTERN.matcher(connectionString.toString())
        if (!matcher.matches()) {
            throw PlcConnectionException(
                "Connection string doesn't match the format '{protocol-code}:({transport-code})?//{transport-address}(?{parameter-string)?'")
        }

        val protocolCode = matcher.group("protocolCode")
        val transportConfig = matcher.group("transportConfig")
        val paramString = matcher.group("paramString")


        if (protocolCode != getProtocolCode()) {
            throw PlcConnectionException("This driver is not suited to handle this connection string")
        }

        val configuration =
            ConfigurationFactory().createConfiguration(configurationType, paramString) ?: throw PlcConnectionException("Unsupported configuration")

        // As the S7 protocol always uses TCP create it statically
        val transport = TcpTransport()

        ConfigurationFactory.configure(configuration, transport)

        // Create communication channel for the driver
        val channelFactory = transport.createChannelFactory(transportConfig)
                             ?: throw PlcConnectionException("Unable to get channel factory from url $transportConfig")
        ConfigurationFactory.configure(configuration, channelFactory)

        initializePipeline(channelFactory)

        val awaitSetupComplete = if (System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_SETUP_COMPLETE) != null)
            parseBoolean(System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_SETUP_COMPLETE)) else awaitSetupComplete()

        val awaitDisconnectComplete = if (System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_DISCONNECT_COMPLETE) != null)
            parseBoolean(System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_DISCONNECT_COMPLETE))
        else awaitDisconnectComplete()

        val awaitDiscoverComplete = if (System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_DISCOVER_COMPLETE) != null)
            parseBoolean(System.getProperty(PROPERTY_PLC4X_FORCE_AWAIT_DISCOVER_COMPLETE))
        else awaitDiscoverComplete()


        return DefaultNettyPlcConnection(
            canRead(), canWrite(), canSubscribe(),
            fieldHandler,
            valueHandler,
            configuration,
            channelFactory,
            awaitSetupComplete,
            awaitDisconnectComplete,
            awaitDiscoverComplete,
            getStackConfigurer(transport),
            optimizer)
    }

    companion object {
        private val URI_PATTERN = Pattern.compile(
            "^(?<protocolCode>[a-z0-9\\-]*)(:(?<transportCode>[a-z0-9]*))?://(?<transportConfig>[^?]*)(\\?(?<paramString>.*))?")
    }
}