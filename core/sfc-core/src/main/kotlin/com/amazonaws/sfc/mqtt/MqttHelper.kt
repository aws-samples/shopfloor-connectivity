package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.crypto.KeyHelpers
import com.amazonaws.sfc.log.Logger
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MqttHelper(private val mqttConnectionConfig: MqttConnectionOptions, private val logger: Logger) {

    private val className = this::class.java.name

    private val trustManagerFactory: TrustManagerFactory by lazy {

        val log = logger.getCtxLoggers(className, "trustManagerFactory")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)

        val sslServerCertificate = sslServerCert

        log.trace("Server SSL certificate is $sslServerCertificate")

        sslServerCertificate.forEachIndexed { i, c ->
            keyStore.setCertificateEntry("server_cert_$i", c)
        }

        if (mqttConnectionConfig.protocol == MqttConnectionProtocol.SSL && mqttConnectionConfig.rootCA != null) {
            log.info("Loading CA certificate from ${mqttConnectionConfig.rootCA?.absolutePath}")
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val caCerts = certificateFactory.generateCertificates(mqttConnectionConfig.rootCA!!.inputStream())
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

        log.info("Loading certificates from certificate file ${mqttConnectionConfig.certificate?.absolutePath} ")
        val certificateChain = certificateFactory.generateCertificates(mqttConnectionConfig.certificate?.inputStream())
        certificateChain.forEach { c ->
            log.trace("Loaded certificate is ${c}")
        }

        log.trace("Loading private key from ${mqttConnectionConfig.privateKey?.absolutePath}")
        val privateKey = mqttConnectionConfig.privateKey?.let { KeyHelpers.loadPrivateKey(it) }
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
        options.connectionTimeout = mqttConnectionConfig.connectTimeout.inWholeSeconds.toInt()

        if (mqttConnectionConfig.password != null) {
            options.password = mqttConnectionConfig.password!!.toCharArray()
        }

        if (mqttConnectionConfig.username != null) {
            options.userName = mqttConnectionConfig.username
        }
        options
    }

    private val sslContext: SSLContext? by lazy {
        if (mqttConnectionConfig.protocol == MqttConnectionProtocol.SSL) {
            val sslContext = SSLContext.getInstance("TLSv1.3")
            sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
            sslContext
        } else null
    }


    private val sslServerCert: MutableList<X509Certificate> by lazy {

        val log = logger.getCtxLoggers(className, "sslServerCert")

        // use certificate from certificate file if configured
        if (mqttConnectionConfig.sslServerCert != null) {
            log.info("Loading server certificate from file ${mqttConnectionConfig.sslServerCert!!.absolutePath}")
            CertificateHelper.loadX509Certificates(mqttConnectionConfig.sslServerCert!!).toMutableList()
        }

        log.info("Loading server certificate from  ${mqttConnectionConfig.endPoint}:${mqttConnectionConfig.port}")
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
        (sslContext.socketFactory.createSocket(mqttConnectionConfig.address, mqttConnectionConfig.port ?: 443) as SSLSocket).use { sslSocket ->
            sslSocket.useClientMode = true
            sslSocket.startHandshake()
            serverCertificates
        }
        serverCertificates
    }

    fun buildClient(clientID: String, persistence: MqttClientPersistence = MemoryPersistence()): MqttClient {

        val log = logger.getCtxLoggers(className, "mqttClient")

        try {
            log.trace("Building mqtt client")
            val options = connectionOptions
            val client = MqttClient(mqttConnectionConfig.endPoint, clientID, persistence)
            log.trace("Connecting to ${mqttConnectionConfig.endPoint}:${mqttConnectionConfig.port}")
            client.connect(options)
            return client
        } catch (e: Exception) {
            logger.getCtxErrorLogEx(className, "client")("Error building MQTT client", e)
            throw e
        }
    }

}