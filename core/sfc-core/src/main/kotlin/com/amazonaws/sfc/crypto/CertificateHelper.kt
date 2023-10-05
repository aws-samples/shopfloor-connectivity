/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.config.SelfSignedCertificateConfig
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.add
import com.amazonaws.sfc.system.DateTime.systemDateUTC
import com.amazonaws.sfc.util.constrainFilePermissions
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import sun.security.x509.GeneralNameInterface
import sun.security.x509.URIName
import sun.security.x509.X509CertImpl
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.net.URI
import java.security.KeyPair
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.time.Period
import kotlin.io.path.Path
import kotlin.io.path.exists

val X509CertImpl.subjectAlternativeApplicationName: String?
    get() {
        val uri = this.subjectAlternativeApplicationUri
        return uri?.schemeSpecificPart
    }

val X509CertImpl.subjectAlternativeApplicationUri: URI?
    get() {
        val subjectNameUri =
            this.subjectAlternativeNameExtension?.get("subject_name")?.names()?.firstOrNull {
                it.name.type == GeneralNameInterface.NAME_URI
            }
        return (subjectNameUri?.name as URIName?)?.uri
    }

open class CertificateHelper(protected val config: CertificateConfiguration, protected val logger: Logger) {

    private val className = this::class.java.simpleName

    fun getCertificateAndKeyPair(): Pair<X509Certificate?, KeyPair?> {

        val trace = logger.getCtxTraceLog(className, "getCertificateAndKeyPair")


        val certificateFileName = config.certificatePath
        if (certificateFileName == null) {
            trace("No certificate configured")
            return null to null
        }

        if (Path(certificateFileName).exists()) {
            return loadCertificateAndKeyPair()
        }

        if (config.selfSignedCertificateConfig == null) {
            throw CertificateException("Certificate file \"$certificateFileName does not exist, specify an existing file or add configuration to create a sef signed certificate")
        }

        val keyPair = try {
            loadOrCreateKeyPair()
        } catch (e: Exception) {
            throw CertificateException("Failed to create or load key pair, $e")

        }

        val certificate = try {
            createCertificate(keyPair, config.selfSignedCertificateConfig!!)

        } catch (e: Exception) {
            throw CertificateException("Failed to create self signed certificate, $e")
        }

        trace("New self signed certificate created, $certificate")
        saveCertificate(certificate, keyPair)
        return certificate to keyPair
    }


    protected open fun saveCertificate(certificate: X509Certificate, keyPair: KeyPair) {
        val info = logger.getCtxInfoLog(className, "saveCertificate")
        val certificatePath = config.certificatePath ?: throw CertificateException("Path of certificate file not set")
        try {
            val certificateFile = File(certificatePath)
            val pemWriter = JcaPEMWriter(FileWriter(certificateFile))
            pemWriter.use {
                pemWriter.writeObject(certificate)
            }
            constrainFilePermissions(certificateFile)
            info("Saved new certificate in PEM format to $certificatePath, $certificate")
        } catch (e: Exception) {
            throw CertificateException("Can not save certificate to $certificatePath, $e")
        }
    }


    protected open fun loadCertificateAndKeyPair(): Pair<X509Certificate?, KeyPair?> {

        val certificateFile = config.certificatePath ?: return null to null

        val trace = logger.getCtxTraceLog(className, "loadCertificateAndKeyPair")

        try {
            trace("Loading certificate from existing certificate file \"${certificateFile}\"")
            val certificate = loadX509Certificates(File(certificateFile)).firstOrNull()
            val keyPair: KeyPair? = if (certificate != null) keyPairFromConfig() else null
            return certificate to keyPair

        } catch (e: Exception) {
            throw CertificateException("Error loading certificate from configuration, $e")
        }

    }

    private fun keyPairFromConfig(): KeyPair? {

        val trace = logger.getCtxTraceLog(className, "keyPairFromConfig")

        if (config.keyPath == null) throw CertificateException("Name of key file is not set")

        return try {
            val keyPath = config.keyPath
            if (keyPath == null || (!Path(keyPath).exists())) return null
            trace("Loading existing key pair from \"$keyPath\"")
            KeyHelpers.loadPkcsKeyPair(File(keyPath))
        } catch (e: Exception) {
            throw CertificateException("Error loading key pair, $e")
        }

    }

    open fun loadOrCreateKeyPair(): KeyPair = loadOrCreateKeyPair(true)

    protected open fun loadOrCreateKeyPair(doSave: Boolean): KeyPair {

        val log = logger.getCtxLoggers(className, "loadOrCreateKeyPair")

        val keyFile = config.keyPath
        if (keyFile == null && doSave) throw CertificateException("Name of key file must be set")

        if (keyFile?.let { Path(it).exists() } == true) {
            val keyPair = keyPairFromConfig()
            if (keyPair != null) return keyPair
        }

        log.trace("Creating new key pair")
        val newKeyPair = KeyHelpers.createRsaKeyPair()
        if (doSave) {
            try {
                val file = File(keyFile!!)
                val pemWriter = JcaPEMWriter(FileWriter(file))
                pemWriter.use {
                    pemWriter.writeObject(newKeyPair.private)
                }
                constrainFilePermissions(file)
                log.info("Saved new key in PEM format to file \"$keyFile\"")
            } catch (e: Exception) {
                throw CertificateException("Error saving key to $keyFile, $e")
            }
        }
        return newKeyPair
    }

    private fun createCertificate(keyPair: KeyPair, certificateConfig: SelfSignedCertificateConfig): X509Certificate {

        val generator = SelfSignedCertificateGenerator()

        val notBefore = systemDateUTC()
        val notAfter = notBefore.add(Period.ofDays(certificateConfig.validityPeriodDays))
        return generator.generate(keyPair, notBefore, notAfter, certificateConfig)
    }

    companion object {

        fun loadX509Certificates(certificateBytes: ByteArray): List<X509Certificate> {
            val factory = CertificateFactory.getInstance("X.509")
            return factory.generateCertificates(certificateBytes.inputStream()).map { it as X509Certificate }
        }

        fun loadX509Certificates(certificateFile: File): List<X509Certificate> {
            return loadX509Certificates(certificateFile.readBytes())
        }

        fun loadAllCertificatesInDir(directory: File,
                                     onLoaded: ((File, List<X509Certificate>) -> Unit)? = null,
                                     onFailed: ((File, Exception) -> Unit)? = null) =
            directory.listFiles()?.filter { f -> f.canRead() && !f.isHidden }?.flatMap { file ->
                try {
                    val certificates = loadX509Certificates(file)
                    if (onLoaded != null) {
                        onLoaded(file, certificates)
                    }
                    certificates
                } catch (e: Exception) {
                    if (onFailed != null) {
                        onFailed(file, e)
                    }
                    emptyList()
                }
            }?.toSet() ?: emptySet()

        fun loadAllCRLsInDir(directory: File, onLoaded: ((File, List<X509CRL>) -> Unit)? = null, onFailed: ((File, Exception) -> Unit)? = null) =
            directory.listFiles()?.filter { f -> f.canRead() && !f.isHidden }?.flatMap { file ->
                try {
                    val factory = CertificateFactory.getInstance("X.509")
                    val crls = factory.generateCRLs(FileInputStream(file)).filterIsInstance<X509CRL>().map { it }
                    if (onLoaded != null) {
                        onLoaded(file, crls)
                    }
                    crls
                } catch (e: Exception) {
                    if (onFailed != null) {
                        onFailed(file, e)
                    }
                    emptyList()
                }
            }?.toSet() ?: emptySet()
    }


}