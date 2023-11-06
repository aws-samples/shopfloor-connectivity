
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc


import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.crypto.KeyHelpers
import com.amazonaws.sfc.crypto.KeyHelpers.asPKCS8
import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ServerConnectionType
import com.amazonaws.sfc.service.ServiceCommandLineOptions
import com.amazonaws.sfc.util.getIp4NetworkAddress
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolNames
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder
import sun.security.rsa.RSAPrivateCrtKeyImpl
import java.io.File
import java.net.InetSocketAddress


abstract class IpcBaseService(val serverConfig: ServerConfiguration, val logger: Logger) {

    protected abstract val serviceImplementation: BindableService

    private var certificateExpiryChecker: CertificateExpiryChecker? = null

    // shadow server property to access server without actually creating it
    protected var _server: Server? = null

    // the grpc server
    protected val grpcServer: Server by lazy {
        _server = createGrpcServer()
        return@lazy _server!!
    }

    private val className = this::class.java.toString()

    private fun createGrpcServer(): Server {
        val log = logger.getCtxLoggers(className, "createGrpcServer")
        val serviceAddress = InetSocketAddress(serverConfig.address, serverConfig.port)

        val serverConnectionType = serverConfig.serverConnectionType

        val builder = NettyServerBuilder.forAddress(serviceAddress)
            .addService(serviceImplementation)

        val usedCertificates = mutableListOf<File>()

        try {
            val serverCertificateFile = serverConfig.serverCertificate
            val serverPrivateKeyFile = serverConfig.serverPrivateKey
            when (serverConnectionType) {

                ServerConnectionType.ServerSideTLS -> {

                    log.trace("Setting server transport security for server side TLS " +
                              "using server certificate file \"${serverCertificateFile?.absolutePath}\", " +
                              "and server private key file \"${serverPrivateKeyFile?.absolutePath}\" ")

                    builder.setupServerSideTLSForServer(serverCertificateFile, serverPrivateKeyFile)
                    serverCertificateFile?.let { usedCertificates.add(it) }
                }

                ServerConnectionType.MutualTLS -> {

                    val caCertificateFile = serverConfig.caCertificate
                    log.trace("Setting up mutual TLS " +
                              "using server certificate file \"${serverCertificateFile?.absolutePath}\", " +
                              "server private key file \"${serverPrivateKeyFile?.absolutePath}\" " +
                              "and CA certificate file ${caCertificateFile?.absolutePath}")

                    builder.setupServerSideMutualTLSForServer(serverCertificateFile, serverPrivateKeyFile, caCertificateFile)

                    serverCertificateFile?.let { usedCertificates.add(it) }
                    caCertificateFile?.let { usedCertificates.add(it) }
                }

                else -> {}
            }

            certificateExpiryChecker = CertificateExpiryChecker(usedCertificates, serverConfig.expirationWarningPeriod, logger)

        } catch (e: Exception) {
            log.error("Error setting up TLS for $serverConnectionType, $e")
        }

        return builder.build()
    }

    private fun NettyServerBuilder.setupServerSideTLSForServer(serverCertificateFile: File?, serverPrivateKeyFile: File?) {

        if (serverCertificateFile == null) throw Exception("Required server certificate for server side TLS not specified")
        if (serverPrivateKeyFile == null) throw Exception("Required server private key for server side TLS not specified")

        val serverKey: RSAPrivateCrtKeyImpl? = KeyHelpers.loadPrivateKey(serverPrivateKeyFile) as? RSAPrivateCrtKeyImpl?
        val pkcs8Key = try {
            serverKey?.asPKCS8 ?: ByteArray(0)
        } catch (e: Exception) {
            throw Exception("\"$serverKey\" does not contain a PKCS8 private key or can not be converted to it")
        }

        this.useTransportSecurity(serverCertificateFile.inputStream(), pkcs8Key.inputStream())
    }


    private fun NettyServerBuilder.setupServerSideMutualTLSForServer(serverCertificateFile: File?,
                                                                     serverPrivateKeyFile: File?,
                                                                     caCertificateFile: File?) {

        if (serverCertificateFile == null) throw ProtocolAdapterException("Required server certificate for mutual TLS not specified")
        if (serverPrivateKeyFile == null) throw ProtocolAdapterException("Required server private key for mutual TLS not specified")
        if (caCertificateFile == null) throw ProtocolAdapterException("Required CA certificate key for mutual TLS not specified")

        val s = SslContextBuilder.forServer(serverCertificateFile, serverPrivateKeyFile)
            .applicationProtocolConfig(ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.NPN_AND_ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1))
            .trustManager(caCertificateFile)
            .clientAuth(ClientAuth.NONE)

        this.sslContext(GrpcSslContexts.configure(s).build())
    }

    companion object {

        fun buildServerConfiguration(cmd: IpcServiceCommandLine,
                                     serverConfiguration: ServerConfiguration?): ServerConfiguration {

            val port = getPort(cmd, serverConfiguration)

            val address = getAddress(cmd)


            val key = cmd.key ?: serverConfiguration?.serverPrivateKey?.absolutePath
            val cert = cmd.cert ?: serverConfiguration?.serverCertificate?.absolutePath
            val ca = cmd.ca ?: serverConfiguration?.caCertificate?.absolutePath
            val serverConnectionType = cmd.connectionType ?: serverConfiguration?.serverConnectionType ?: ServerConnectionType.PlainText

            checkConnectionRequirements(serverConnectionType, key, cert, ca)

            val configuration = ServerConfiguration.create(
                address = address,
                port = port,
                serverPrivateKey = key,
                serverCertificate = cert,
                caCertificate = ca,
                serverConnectionType = serverConnectionType,
                usedByServer = true
            )

            configuration.validate()
            return configuration
        }

        private fun getAddress(cmd: IpcServiceCommandLine): String {
            return getIp4NetworkAddress(cmd.networkInterface)
                   ?: throw ProtocolAdapterException("No IP4 network address for interface ${cmd.networkInterface}")
        }

        private fun getPort(cmd: IpcServiceCommandLine, protocolServerConfiguration: ServerConfiguration?): Int {
            return cmd.port ?: protocolServerConfiguration?.port
                   ?: throw ProtocolAdapterException("No IPC service port was not specified on command line, in environment variable or found in configuration file")
        }

        private fun checkConnectionRequirements(serverConnectionType: ServerConnectionType, key: String?, cert: String?, ca: String?) {
            when (serverConnectionType) {
                ServerConnectionType.ServerSideTLS -> checkServerSideTlsRequiredParams(key, cert)
                ServerConnectionType.MutualTLS -> checkMutualTlsRequiredParams(key, cert, ca)
                ServerConnectionType.Unknown -> {
                    throw ProtocolAdapterException("Unknown connection type used for parameter ${ServiceCommandLineOptions.OPTION_CONNECTION_TYPE}, valid values are ${ServerConnectionType.validValues}")
                }

                else -> {}
            }
        }

        private fun checkServerSideTlsRequiredParams(key: String?, cert: String?) {
            checkKeyFileParamPresent(ServerConnectionType.ServerSideTLS, key)
            checkCertificateFilePresent(ServerConnectionType.ServerSideTLS, cert)
        }

        private fun checkMutualTlsRequiredParams(key: String?, cert: String?, ca: String?) {
            checkKeyFileParamPresent(ServerConnectionType.MutualTLS, key)
            checkCertificateFilePresent(ServerConnectionType.MutualTLS, cert)
            checkCaCertificateFilePresent(ca)
        }

        private fun checkCertificateFilePresent(serverConnectionType: ServerConnectionType, cert: String?) {
            if (cert == null) throw ProtocolAdapterException("Certificate file parameter ${ServiceCommandLineOptions.OPTION_CERTIFICATE_FILE} for server must be set for connection type $serverConnectionType")
        }

        private fun checkCaCertificateFilePresent(cert: String?) {
            if (cert == null) throw ProtocolAdapterException("CA Certificate file parameter ${ServiceCommandLineOptions.OPTION_CERTIFICATE_FILE} for server must be set for connection type ${ServerConnectionType.MutualTLS}")
        }

        private fun checkKeyFileParamPresent(serverConnectionType: ServerConnectionType, key: String?) {
            if (key == null) throw ProtocolAdapterException("Private key file parameter ${ServiceCommandLineOptions.OPTION_KEY_FILE} for server must be set for connection type $serverConnectionType")
        }
    }

}


