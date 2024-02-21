// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import MqttConfigProviderConfig
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.crypto.KeyHelpers
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ConfigProvider
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.getHostName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.security.KeyStore
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

@ConfigurationClass
class MqttConfigProvider(
    private val configStr: String,
    private val configVerificationKey: PublicKey?,
    private val logger: Logger
) : ConfigProvider {

    private val className = this::class.java.simpleName

    // channel used to send configurations to SFC-Core
    private val ch = Channel<String>(1)

    private val scope = buildScope("MqttConfigProvider")

    // Get initial JSON config, this config contains the information to connect and subscribe to the topic and a reference to a stored local configuration file
    private val providerConfig: MqttConfigProviderConfig by lazy {
        ConfigReader.createConfigReader(configStr).getConfig(validate = true)
    }

    private val trustManagerFactory: TrustManagerFactory by lazy {

        val log = logger.getCtxLoggers(className, "trustManagerFactory")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)

        val sslServerCertificate = sslServerCert

        log.trace("Server SSL certificate is $sslServerCertificate")

        sslServerCertificate.forEachIndexed { i, c ->
            keyStore.setCertificateEntry("server_cert_$i", c)
        }

        if (providerConfig.connection == Connection.MUTUAL && providerConfig.rootCA != null) {
            log.info("Loading CA certificate from ${providerConfig.rootCA?.absolutePath}")
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val caCerts = certificateFactory.generateCertificates(providerConfig.rootCA!!.inputStream())
            caCerts.forEachIndexed { i, c ->
                keyStore.setCertificateEntry("ca_cert_$i", c)
                log.trace("Loaded CA certificate is $c")
            }
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        trustManagerFactory
    }

    private val keyManagerFactory: KeyManagerFactory by lazy {

        val log = logger.getCtxLoggers(className, "keyManagerFactory")

        val certificateFactory = CertificateFactory.getInstance("X.509")

        log.info("Loading certificates from certificate file ${providerConfig.certificate?.absolutePath} ")
        val certificateChain = certificateFactory.generateCertificates(providerConfig.certificate?.inputStream())
        certificateChain.forEach { c ->
            log.trace("Loaded certificate is ${c.toString()}")
        }

        log.trace("Loading private key from ${providerConfig.privateKey?.absolutePath}")
        val privateKey = providerConfig.privateKey?.let { KeyHelpers.loadPrivateKey(it) }
        log.trace("Loaded private key is ${privateKey.toString()}")

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null)
        keyStore.setKeyEntry("private_key", privateKey, null, certificateChain.toTypedArray<Certificate>())
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        keyManagerFactory
    }

    private val connectionOptions: MqttConnectOptions by lazy {

        val sslContext = sslContext

        val options = MqttConnectOptions()

        if (sslContext != null) {
            options.socketFactory = sslContext.socketFactory
        }
        options.keepAliveInterval = 300
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = providerConfig.connectTimeout

        if (providerConfig.password != null) {
            options.password = providerConfig.password!!.toCharArray()
        }

        if (providerConfig.username != null) {
            options.userName = providerConfig.username
        }
        options
    }

    private val sslContext: SSLContext? by lazy {
        when (providerConfig.connection) {
            Connection.SSL -> {
                val sslContext = SSLContext.getInstance("TLSv1.3")
                sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
                sslContext
            }

            Connection.MUTUAL -> {
                trustManagerFactory to keyManagerFactory
                val sslContext = SSLContext.getInstance("TLSv1.3")
                sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
                sslContext
            }

            Connection.PLAINTEXT -> null
        }
    }

    val mqttClient: MqttClient by lazy {

        val log = logger.getCtxLoggers(className, "mqttClient")

        try {
            log.trace("Building mqtt client")
            val options = connectionOptions
            val client = MqttClient(providerConfig.endpoint, "sfc_config_provider_${getHostName()}")
            log.trace("Connecting to ${providerConfig.endpoint}:${providerConfig.port}")
            client.connect(options)
            client
        } catch (e: Throwable) {
            logger.getCtxErrorLog(className, "client")("$e")
            throw e
        }
    }


    val worker = scope.launch {

        val channel = Channel<String>()
        val log = logger.getCtxLoggers(className, "mqtt config provider")

        val c = mqttClient
        println(c)

        c.subscribe(providerConfig.topicName) { _, message ->
            println(message.payload.toString())
            runBlocking {
                ch.send(String(message.payload))
            }
        }


        while (isActive) {
            val message = channel.receive()
            println("Processing $message")

        }
    }

    private val sslServerCert: MutableList<X509Certificate> by lazy {

        val log = logger.getCtxLoggers(className, "sslServerCert")

        // use certificate from certificate file if configured
        if (providerConfig.sslServerCert != null) {
            log.info("Loading server certificate from file ${providerConfig.sslServerCert!!.absolutePath}")
            CertificateHelper.loadX509Certificates(providerConfig.sslServerCert!!).toMutableList()
        }

        log.info("Loading server certificate from  ${providerConfig.endpoint}:${providerConfig.port}")
        val serverCertificates: MutableList<X509Certificate> = mutableListOf()
        val sslContext = SSLContext.getInstance("TLS")

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
        (sslContext.socketFactory.createSocket(providerConfig.endpoint, providerConfig.port ?: 443) as SSLSocket).use { sslSocket ->
            sslSocket.useClientMode = true
            sslSocket.startHandshake()
            serverCertificates
        }
        serverCertificates
    }


    override val configuration: Channel<String> = ch

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): ConfigProvider {
            return MqttConfigProvider(
                createParameters[0] as String,
                createParameters[1] as PublicKey?,
                createParameters[2] as Logger
            )
        }
    }
}


