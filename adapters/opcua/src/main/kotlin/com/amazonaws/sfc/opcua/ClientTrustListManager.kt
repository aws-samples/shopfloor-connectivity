/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.constrainFilePermissions
import com.amazonaws.sfc.util.ensureExists
import com.google.common.collect.ImmutableList
import io.netty.buffer.ByteBufUtil
import kotlinx.coroutines.Dispatchers
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.eclipse.milo.opcua.stack.core.security.TrustListManager
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString
import org.eclipse.milo.opcua.stack.core.util.DigestUtil
import java.io.Closeable
import java.io.FileWriter
import java.net.URLEncoder
import java.nio.file.Path
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import kotlin.io.path.Path


class ClientTrustListManager(baseDirectoryName: String, private val logger: Logger, onUpdate: (Path) -> Unit) : TrustListManager, Closeable {

    private val className = this::class.simpleName.toString()

    private val basePath = Path(baseDirectoryName)

    private val scope = buildScope("ClientTrustListManager", Dispatchers.IO)

    private val issuersPath: Path = basePath.resolve(ISSUERS_DIR_NAME)
    private val issuerCertificatePath: Path = issuersPath.resolve(CERTIFICATES_DIR_NAME).ensureExists()
    private val issuersClrPath: Path = issuersPath.resolve(CRL_DIR_NAME).ensureExists()

    private val issuerCertificateWatcher = CertificateDirectoryWatcher(issuerCertificatePath, scope, logger) { onUpdate(issuerCertificatePath) }
    private val issuerClrWatcher = CrlDirectoryWatcher(issuersClrPath, scope, logger) { onUpdate(issuersClrPath) }

    private val trustedPath: Path = basePath.resolve(TRUSTED_CERTIFICATE_DIR_NAME)
    private val trustedCertificatePath: Path = trustedPath.resolve(CERTIFICATES_DIR_NAME).ensureExists()
    private val trustedClrPath: Path = trustedPath.resolve(CRL_DIR_NAME).ensureExists()
    private val trustedCertificateWatcher = CertificateDirectoryWatcher(trustedCertificatePath, scope, logger) { onUpdate(trustedCertificatePath) }
    private val trustedClrWatcher = CrlDirectoryWatcher(trustedClrPath, scope, logger) { onUpdate(trustedClrPath) }

    val trustedCertificatesDirectory by lazy { trustedCertificatePath }

    private val rejectedPath = basePath.resolve(REJECTED_DIR_NAME).ensureExists()


    override fun close() {
        issuerClrWatcher.close()
        issuerCertificateWatcher.close()
        trustedClrWatcher.close()
        trustedCertificateWatcher.close()
    }

    private val nop: (Any?) -> Unit = { _ -> }
    override fun getIssuerCrls(): ImmutableList<X509CRL> = ImmutableList.copyOf(issuerClrWatcher.entries)

    override fun getTrustedCrls(): ImmutableList<X509CRL> = ImmutableList.copyOf(trustedClrWatcher.entries)

    override fun getIssuerCertificates(): ImmutableList<X509Certificate> = ImmutableList.copyOf(issuerCertificateWatcher.entries)

    override fun getTrustedCertificates(): ImmutableList<X509Certificate> = ImmutableList.copyOf(trustedCertificateWatcher.entries)

    override fun getRejectedCertificates(): ImmutableList<X509Certificate> {
        return ImmutableList.of()
    }

    private fun writeCertificate(certificate: X509Certificate, destinationDirectory: String) {
        val log = logger.getCtxLoggers(className, "writeCertificate")
        val file = Path(destinationDirectory).resolve(certificate.fileName).toFile()
        try {

            val pemWriter = JcaPEMWriter(FileWriter(file))
            pemWriter.use {
                pemWriter.writeObject(certificate)
            }
            constrainFilePermissions(file)
            log.info("Written certificate to ${file.absolutePath}")
        } catch (e: java.lang.Exception) {

            log.error("Error writing certificate to ${file.absolutePath} $e")
        }
    }

    private val X509Certificate.fileName: String
        get() {
            var name = this.subjectX500Principal.name.split(",").firstOrNull { it.isNotEmpty() }
                       ?: this.subjectX500Principal.name
            name = URLEncoder.encode(name, "UTF-8").replace("*", "_")
            val signatureDigest = ByteBufUtil.hexDump(DigestUtil.sha1(this.signature))
            return "$signatureDigest-$name"
        }


    override fun addRejectedCertificate(certificate: X509Certificate) {
        val log = logger.getCtxLoggers(className, "addRejectedCertificate")
        log.info("Saving rejected certificate $certificate")
        writeCertificate(certificate, rejectedPath.toString())
    }

    override fun setIssuerCrls(issuerCrls: MutableList<X509CRL>?) = nop(issuerCrls)

    override fun setTrustedCrls(trustedCrls: MutableList<X509CRL>?) = nop(trustedCrls)

    override fun setIssuerCertificates(issuerCertificates: MutableList<X509Certificate>?) = nop(issuerCertificates)

    override fun setTrustedCertificates(trustedCertificates: MutableList<X509Certificate>?) = nop(trustedCertificates)

    override fun addIssuerCertificate(certificate: X509Certificate?) = nop(certificate)

    override fun addTrustedCertificate(certificate: X509Certificate?) = nop(certificate)

    override fun removeIssuerCertificate(thumbprint: ByteString?): Boolean = true

    override fun removeTrustedCertificate(thumbprint: ByteString?): Boolean = true

    override fun removeRejectedCertificate(thumbprint: ByteString?): Boolean = true

    companion object {
        private const val REJECTED_DIR_NAME = "rejected"
        private const val ISSUERS_DIR_NAME = "issuers"
        private const val TRUSTED_CERTIFICATE_DIR_NAME = "trusted"
        private const val CERTIFICATES_DIR_NAME = "certs"
        private const val CRL_DIR_NAME = "crl"
    }

}