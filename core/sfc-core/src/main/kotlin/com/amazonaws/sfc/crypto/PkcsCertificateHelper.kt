
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.log.Logger
import java.io.File
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

class PkcsCertificateHelper(config: CertificateConfiguration, logger: Logger) : CertificateHelper(config, logger) {

    private val className = this::class.java.simpleName

    override fun loadCertificateAndKeyPair(): Pair<X509Certificate?, KeyPair?> {

        val certificateFile = (config.certificatePath) ?: return null to null

        val trace = logger.getCtxTraceLog(className, "loadCertificateAndKeyPair")

        return try {

            val keyStore = loadKeyStoreFromFromConfig()

            trace("Getting certificate")
            val clientPrivateKey = keyStore.getKey(config.alias, config.password?.toCharArray())
            if (clientPrivateKey is PrivateKey) {
                trace("Getting keypair from certificate")
                val clientCertificate = keyStore.getCertificate(config.alias) as X509Certificate
                clientCertificate to KeyPair(clientCertificate.publicKey, clientPrivateKey)
            } else {
                null to null
            }

        } catch (e: Exception) {
            throw CertificateException("Can not load certificate and key pair from PKCS12 keystore file  ${certificateFile}, $e")
        }
    }

    private fun loadKeyStoreFromFromConfig(): KeyStore {
        val trace = logger.getCtxTraceLog(className, "certificateStreamFromConfig")

        val certificateFile = config.certificatePath ?: throw CertificateException("Name of certificate file is not set")

        trace("Loading certificate from file \"$certificateFile\"")
        val bytes = File(certificateFile).readBytes()
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(bytes.inputStream(), config.password?.toCharArray())
        return keyStore
    }

    override fun loadOrCreateKeyPair(): KeyPair = loadOrCreateKeyPair(false)

    override fun saveCertificate(certificate: X509Certificate, keyPair: KeyPair) {

        val trace = logger.getCtxTraceLog(className, "saveCertificate")

        val certificateFile = config.certificatePath ?: throw CertificateException("name of certificate file not set")

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, config.password?.toCharArray())
        keyStore.setKeyEntry(config.alias, keyPair.private, config.password?.toCharArray(), arrayOf(certificate))

        File(certificateFile).outputStream().use {
            keyStore.store(it, config.password?.toCharArray())
        }
        trace("Certificate with key pair saved to file \"$certificateFile\"")
    }

}