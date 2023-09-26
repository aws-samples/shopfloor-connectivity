package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.config.SelfSignedCertificateConfig
import com.amazonaws.sfc.util.getAddressesAndHostNames
import com.amazonaws.sfc.util.getHostName
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.util.*

class SelfSignedCertificateGenerator {

    private fun X500NameBuilder.addIfNotNull(identifier: ASN1ObjectIdentifier, value: String?): X500NameBuilder {
        if (value != null) this.addRDN(identifier, value)
        return this
    }

    private fun X509v3CertificateBuilder.addBasicConstraints(): X509v3CertificateBuilder =
        this.addExtension(Extension.basicConstraints, false, BasicConstraints(true))

    private fun X509v3CertificateBuilder.addAuthorityKeyIdentifier(keyPair: KeyPair): X509v3CertificateBuilder =
        this.addExtension(
            Extension.authorityKeyIdentifier,
            false,
            JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.public)
        )

    private fun X509v3CertificateBuilder.addKeyUsage(): X509v3CertificateBuilder =
        this.addExtension(Extension.keyUsage, false, KEY_USAGE)


    private fun X509v3CertificateBuilder.addExtendedKeyUsage(): X509v3CertificateBuilder {

        return this.addExtension(
            Extension.extendedKeyUsage, false, EXTENDED_KEY_USAGE
        )
    }

    private fun X509v3CertificateBuilder.addSubjectAlternativeNames(
        dnsNames: List<String>?,
        ipAddresses: List<String>?
    ): X509v3CertificateBuilder {

        val generalNames = mutableListOf<GeneralName>()

        val applicationUri = "urn:aws-sfc-opcua@${getHostName()}"
        generalNames.add(GeneralName(GeneralName.uniformResourceIdentifier, applicationUri))

        val (hostIpAddresses, hostDnsNames) = if (dnsNames == null || ipAddresses == null) getAddressesAndHostNames() else null to null

        generalNames.add(GeneralName(GeneralName.uniformResourceIdentifier, applicationUri))
        val dnsNamesToAdd = if (dnsNames == null) hostDnsNames else if (dnsNames.isNotEmpty()) dnsNames else null
        dnsNamesToAdd?.toSet()?.let { generalNames.addAll(it.map { dns -> GeneralName(GeneralName.dNSName, dns) }) }

        val ipAddressesToAdd = if (ipAddresses == null) hostIpAddresses else if (ipAddresses.isNotEmpty()) ipAddresses else null
        ipAddressesToAdd?.toSet()?.let { generalNames.addAll(it.map { ip -> GeneralName(GeneralName.iPAddress, ip) }) }

        this.addExtension(
            Extension.subjectAlternativeName, false, GeneralNames(generalNames.toTypedArray())
        )

        return this
    }

    private fun X509v3CertificateBuilder.addSubjectKeyIdentifier(keyPair: KeyPair): X509v3CertificateBuilder =
        this.addExtension(
            Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.public)
        )

    fun generate(
        keyPair: KeyPair,
        notBefore: Date,
        notAfter: Date,
        certificateConfig: SelfSignedCertificateConfig
    ): java.security.cert.X509Certificate {

        val signatureAlgorithm = when (keyPair.public) {
            is java.security.interfaces.RSAPublicKey -> "SHA256withRSA"
            is java.security.interfaces.DSAPublicKey -> throw CertificateException("Only RSA and EC key pairs are supported")
            is java.security.interfaces.ECPublicKey -> "SHA256withECDSA"
            else -> "SHA256withRSA"
        }

        val issuerName = X500NameBuilder()
            .addIfNotNull(BCStyle.CN, certificateConfig.commonName)
            .addIfNotNull(BCStyle.O, certificateConfig.organization)
            .addIfNotNull(BCStyle.OU, certificateConfig.organizationalUnit)
            .addIfNotNull(BCStyle.L, certificateConfig.localityName)
            .addIfNotNull(BCStyle.ST, certificateConfig.stateName)
            .addIfNotNull(BCStyle.C, certificateConfig.countryCode).build()

        // Using the current timestamp as the certificate serial number
        val serial = BigInteger(System.currentTimeMillis().toString())

        val subjectPublicKey: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(
            keyPair.public.encoded
        )

        val certificateBuilder = X509v3CertificateBuilder(
            issuerName, serial, notBefore, notAfter,
            Locale.ENGLISH, issuerName, subjectPublicKey
        )
            .addBasicConstraints()
            .addAuthorityKeyIdentifier(keyPair)
            .addKeyUsage()
            .addExtendedKeyUsage()
            .addSubjectAlternativeNames(
                certificateConfig.dnsNames,
                certificateConfig.ipAddresses
            )
            .addSubjectKeyIdentifier(keyPair)

        val contentSigner: ContentSigner = JcaContentSignerBuilder(signatureAlgorithm)
            .setProvider(BouncyCastleProvider())
            .build(keyPair.private)

        val certificateHolder: X509CertificateHolder = certificateBuilder.build(contentSigner)
        return JcaX509CertificateConverter().getCertificate(certificateHolder)
    }

    companion object {

        val KEY_USAGE = KeyUsage(
            KeyUsage.digitalSignature or
                    KeyUsage.keyAgreement or
                    KeyUsage.keyCertSign or
                    KeyUsage.keyEncipherment or
                    KeyUsage.nonRepudiation or
                    KeyUsage.dataEncipherment
        )

        val EXTENDED_KEY_USAGE = ExtendedKeyUsage(
            arrayOf<KeyPurposeId>(
                KeyPurposeId.id_kp_clientAuth,
                KeyPurposeId.id_kp_serverAuth
            )
        )
    }
}


