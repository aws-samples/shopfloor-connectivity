// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.log.Logger
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class SSLHelper(private val sslConfiguration: TlsConfiguration, private val logger: Logger) {

    private val className = this::class.java.name

    val trustManagerFactory: TrustManagerFactory by lazy {

        val log = logger.getCtxLoggers(className, "trustManagerFactory")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)

        val sslServerCertificate = sslServerCert

        log.trace("Server SSL certificate is $sslServerCertificate")

        sslServerCertificate.forEachIndexed { i, c ->
            keyStore.setCertificateEntry("server_cert_$i", c)
        }

        if (sslConfiguration.rootCA != null) {
            log.info("Loading CA certificate from ${sslConfiguration.rootCA?.absolutePath}")
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val caCerts = certificateFactory.generateCertificates(sslConfiguration.rootCA!!.inputStream())
            caCerts.forEachIndexed { i, c ->
                keyStore.setCertificateEntry("ca_cert_$i", c)
                log.trace("Loaded CA certificate is $c")
            }
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        trustManagerFactory
    }

    val keyManagerFactory: KeyManagerFactory by lazy {

        val log = logger.getCtxLoggers(className, "keyManagerFactory")

        val certificateFactory = CertificateFactory.getInstance("X.509")

        log.info("Loading certificates from certificate file ${sslConfiguration.certificate?.absolutePath} ")
        val certificateChain = certificateFactory.generateCertificates(sslConfiguration.certificate?.inputStream())
        certificateChain.forEach { c ->
            log.trace("Loaded certificate is $c")
        }

        log.trace("Loading private key from ${sslConfiguration.privateKey?.absolutePath}")
        val privateKey = sslConfiguration.privateKey?.let { KeyHelpers.loadPrivateKey(it) }
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

    val sslContext: SSLContext? by lazy {
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
        sslContext
    }


    private val sslServerCert: MutableList<X509Certificate> by lazy {

        val log = logger.getCtxLoggers(className, "sslServerCert")

        if (sslConfiguration.sslServerCert != null) {
            log.info("Loading server certificate from file ${sslConfiguration.sslServerCert!!.absolutePath}")
            CertificateHelper.loadX509Certificates(sslConfiguration.sslServerCert!!).toMutableList()
        } else {
            mutableListOf<X509Certificate>()
        }
    }


}