/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ServerConnectionType
import com.amazonaws.sfc.util.getIp4Address
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.TlsChannelCredentials
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.*

class IpcClientBuilder {


    companion object {

        private val className = this::class.java.simpleName.toString()

        fun <T> createIpcClient(serverConfig: ServerConfiguration, logger: Logger, createClient: (channel: ManagedChannel) -> T): T {

            return try {

                val log = logger.getCtxLoggers("IpcClientBuilder", "createIpcClient")
                log.info("Creating client to connect to IPC service ${serverConfig.addressStr} using connection type ${serverConfig.serverConnectionType}")

                val channel = buildManagedChannel(serverConfig, logger)
                // create the actual client by calling the createClient parameter function with a configured channel
                createClient(channel)

            } catch (e: Exception) {
                throw Exception("Error creating IPC client, ${e.message}")
            }
        }

        private fun buildChannelCredentials(serverConfig: ServerConfiguration, logger: Logger): ChannelCredentials? =
            when (serverConfig.serverConnectionType) {
                ServerConnectionType.ServerSideTLS -> buildServerSideTlsCredentials(serverConfig, logger)
                ServerConnectionType.MutualTLS -> buildMutualTlsCredentials(logger, serverConfig)
                else -> null
            }

        private fun buildServerSideTlsCredentials(serverConfig: ServerConfiguration, logger: Logger): ChannelCredentials? {

            val serverCertificate = getServerCertificate(serverConfig, logger)

            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            trustStore.load(null, null)
            trustStore.setCertificateEntry("ca", serverCertificate.first())

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)
            val trustManager = trustManagerFactory.trustManagers.first()

            return TlsChannelCredentials.newBuilder()
                .trustManager(trustManager).build()
        }

        private fun getServerCertificate(
            serverConfig: ServerConfiguration,
            logger: Logger): List<X509Certificate> {
            val log = logger.getCtxLoggers(className, "getServerCertificate")

            val certificateFile = serverConfig.serverCertificate
            val serverCertificate = if (certificateFile != null) {
                try {
                    CertificateHelper.loadX509Certificates(certificateFile)
                } catch (e: Exception) {
                    throw Exception("Could not load certificate from file ${certificateFile.absolutePath}")
                }
            } else {
                log.trace("Getting server certificates from ${serverConfig.addressStr}")
                try {
                    val certificateFromServer = getServerSideTlsCertificates(serverConfig, logger)
                    if (certificateFromServer.isEmpty()) {
                        throw Exception("Server at ${serverConfig.addressStr} did not return the server certificate required for connection type ${serverConfig.serverConnectionType}, " +
                                        "configure server certificate or use connection type ${ServerConnectionType.PlainText}")
                    }
                    certificateFromServer
                } catch (e: Exception) {
                    throw Exception("Could not retrieve required server certificate for connection type  ${serverConfig.serverConnectionType} from ${serverConfig.addressStr}, $e")
                }
            }
            return serverCertificate
        }

        private fun buildMutualTlsCredentials(logger: Logger,
                                              serverConfig: ServerConfiguration): ChannelCredentials? {
            val log = logger.getCtxLoggers(className, "mutualTlsCredentials")

            val clientPrivateKeyFile =
                serverConfig.clientPrivateKey ?: throw ProtocolAdapterException("Required client private key for mutual TLS not specified")
            val clientCertificateFile =
                serverConfig.clientCertificate ?: throw ProtocolAdapterException("Required client certificate key for mutual TLS not specified")
            val caCertificateFile = serverConfig.caCertificate ?: throw ProtocolAdapterException("Required CA certificate for mutual TLS not specified")

            log.trace("Setting up mutual TLS " +
                      "using client certificate file \"${clientCertificateFile.absolutePath}\", " +
                      "client private key file \"${clientPrivateKeyFile.absolutePath}\" " +
                      "and CA certificate file ${caCertificateFile.absolutePath}")

            return TlsChannelCredentials.newBuilder()
                .keyManager(clientCertificateFile, clientPrivateKeyFile)
                .trustManager(caCertificateFile).build()
        }


        private fun buildManagedChannel(serverConfig: ServerConfiguration,
                                        logger: Logger): ManagedChannel {

            val serverAddress = getIp4Address(serverConfig.address) ?: throw Exception("Can not resolve server address \"${serverConfig.address}\"")

            val channelCredentials = buildChannelCredentials(serverConfig, logger)

            val channelBuilder = if (channelCredentials != null)
                Grpc.newChannelBuilderForAddress(serverConfig.address, serverConfig.port, channelCredentials)
            else
                NettyChannelBuilder
                    .forAddress(serverAddress, serverConfig.port)
                    .usePlaintext()

            channelBuilder.executor(Dispatchers.Default.asExecutor())
            return channelBuilder.build()
        }


        private fun getServerSideTlsCertificates(serverConfig: ServerConfiguration, logger: Logger): MutableList<X509Certificate> {

            val log = logger.getCtxLoggers("IpcClientBuilder", "setupMutualTlsConnection")
            log.trace("Getting server certificates from ${serverConfig.addressStr}")

            val serverCertificates: MutableList<X509Certificate> = mutableListOf()
            val sslContext = SSLContext.getInstance("TLSv1.2")

            // setup ssl context
            sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {

                override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                    serverCertificates.add(x509Certificates[0])
                }

                override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                    serverCertificates.add(x509Certificates[0])
                }

                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }
            }), null)

            // request certificates over socket connection
            (sslContext.socketFactory.createSocket(serverConfig.address, serverConfig.port) as SSLSocket).use { sslSocket ->
                sslSocket.useClientMode = true
                sslSocket.startHandshake()
                return serverCertificates
            }
        }
    }

}