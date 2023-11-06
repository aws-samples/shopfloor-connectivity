
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util


import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS
import com.amazonaws.sfc.config.ClientConfiguration
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.crypto.CertificateHelper
import com.amazonaws.sfc.crypto.CryptoException
import com.amazonaws.sfc.crypto.KeyHelpers
import software.amazon.awssdk.http.apache.ApacheHttpClient
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

object ClientConfigurationUtils {
    @JvmStatic
    fun getConfiguredClientBuilder(deviceConfiguration: ClientConfiguration): ApacheHttpClient.Builder {
        val httpClient = ProxyUtils.sdkHttpClientBuilder
        try {
            configureClientMutualTLS(httpClient, deviceConfiguration)
        } catch (e: Exception) {
            throw ConfigurationException("Error during configure client mutual auth, $e", CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS)
        }
        return httpClient
    }

    private fun configureClientMutualTLS(
        httpBuilder: ApacheHttpClient.Builder,
        clientConfiguration: ClientConfiguration) {

        if (!clientConfiguration.hasMTLSRequiredCertificatesAndKey()) {
            return
        }

        val rootCaBytes: ByteArray = clientConfiguration.rootCABytes ?: ByteArray(0)
        val privateKeyBytes: ByteArray = clientConfiguration.privateKeyBytes ?: ByteArray(0)
        val certificateBytes: ByteArray = clientConfiguration.deviceCertificateBytes ?: ByteArray(0)

        val trustManagers = createTrustManagers(rootCaBytes)
        val keyManagers = createKeyManagers(privateKeyBytes, certificateBytes)
        httpBuilder.tlsKeyManagersProvider { keyManagers }.tlsTrustManagersProvider { trustManagers }
    }

    @Throws(Exception::class)
    private fun createTrustManagers(rootCABytes: ByteArray): Array<TrustManager> {
        return try {
            val trustCertificates = CertificateHelper.loadX509Certificates(rootCABytes)
            val tmKeyStore = KeyStore.getInstance("JKS")
            tmKeyStore.load(null, null)
            for (certificate in trustCertificates) {
                val principal = certificate.subjectX500Principal
                val name = principal.getName("RFC2253")
                tmKeyStore.setCertificateEntry(name, certificate)
            }
            val trustManagerFactory = TrustManagerFactory.getInstance("X509")
            trustManagerFactory.init(tmKeyStore)
            trustManagerFactory.trustManagers
        } catch (e: GeneralSecurityException) {
            val message = "Failed to create trust manager due to general security exception, $e"
            throw CryptoException(message)
        } catch (e: IOException) {
            val message = "IO error creating trust manager, $e"
            throw CryptoException(message)
        }
    }

    @Throws(Exception::class)
    private fun createTrustManagers(rootCAFile: File): Array<TrustManager> {
        val bytes = rootCAFile.readBytes()
        return createTrustManagers(bytes)
    }

    private fun createKeyManagers(privateKeyBytes: ByteArray, certificateBytes: ByteArray): Array<KeyManager> {
        return try {
            val certificateChain = CertificateHelper.loadX509Certificates(certificateBytes)
            val privateKey = KeyHelpers.loadPrivateKey(privateKeyBytes)
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null)
            keyStore.setKeyEntry("private-key", privateKey, null, certificateChain.toTypedArray<Certificate>())
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, null)
            keyManagerFactory.keyManagers
        } catch (e: GeneralSecurityException) {
            val message = "Failed to create key manager due to general security issue, $e"
            throw ConfigurationException(message, CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS)
        } catch (e: IOException) {
            val message = "Failed to create key manager due to IO error, $e"
            throw ConfigurationException(message, CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS)
        }
    }

    private fun createKeyManagers(privateKeyFile: File, certificateFile: File): Array<KeyManager> {
        val keyBytes = privateKeyFile.readBytes()
        val certificateBytes = certificateFile.readBytes()
        return createKeyManagers(keyBytes, certificateBytes)
    }
}