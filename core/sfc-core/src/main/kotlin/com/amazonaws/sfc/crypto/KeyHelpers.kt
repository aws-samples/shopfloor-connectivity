
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.util.constrainFilePermissions
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import sun.security.rsa.RSAPrivateCrtKeyImpl
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.*
import java.util.*

// -----BEGIN RSA PRIVATE KEY-----
@Suppress("MemberVisibilityCanBePrivate") object KeyHelpers {
    private const val PKCS_1_PEM_HEADER = "-----BEGIN RSA % KEY-----"
    private const val PKCS_1_PEM_FOOTER = "-----END RSA % KEY-----"
    private const val PKCS_8_PEM_HEADER = "-----BEGIN % KEY-----"
    private const val PKCS_8_PEM_FOOTER = "-----END % KEY-----"
    private const val PKCS_8_EC_HEADER = "-----BEGIN EC % KEY-----"
    private const val PKCS_8_EC_FOOTER = "-----END EC % KEY-----"

    private const val PUBLIC_KEY = "PUBLIC"
    private const val PRIVATE_KEY = "PRIVATE"


    private fun <T> loadKeyData(keyBytes: ByteArray,
                                keyType: String,
                                fnReadPkcs1: (ByteArray) -> T,
                                fnReadPkcs8: (ByteArray) -> T): T {
        var keyString = String(keyBytes, StandardCharsets.UTF_8)

        val pkcs1Header = PKCS_1_PEM_HEADER.replace("%", keyType)
        if (keyString.contains(pkcs1Header)) {
            keyString = keyString
                .replace(pkcs1Header, "")
                .replace(System.lineSeparator(), "")
                .replace(PKCS_1_PEM_FOOTER.replace("%", keyType), "")
            return fnReadPkcs1(Base64.getMimeDecoder().decode(keyString))
        }
        var pkcs8Header = PKCS_8_PEM_HEADER.replace("%", keyType)
        if (keyString.contains(pkcs8Header)) {
            keyString = keyString.replace(pkcs8Header, "")
                .replace(System.lineSeparator(), "")
                .replace(PKCS_8_PEM_FOOTER.replace("%", keyType), "")
            return fnReadPkcs8(Base64.getMimeDecoder().decode(keyString))
        }

        pkcs8Header = PKCS_8_EC_HEADER.replace("%", keyType)
        if (keyString.contains(pkcs8Header)) {
            keyString = keyString.replace(pkcs8Header, "")
                .replace(System.lineSeparator(), "")
                .replace(PKCS_8_EC_FOOTER.replace("%", keyType), "")
            return fnReadPkcs8(Base64.getMimeDecoder().decode(keyString.toByteArray()))
        }

        return fnReadPkcs8(keyBytes)
    }

    fun loadPkcsKeyPair(keyFile: File): KeyPair = loadKeyPkcsKeyPair(keyFile.readBytes())

    fun loadKeyPkcsKeyPair(keyBytes: ByteArray): KeyPair =
        loadKeyData(keyBytes, PRIVATE_KEY,
            { b -> readPkcs1PrivateKeyPair(b) },
            { b -> readPkcs8KeyPair(b) })


    private fun readPkcs8KeyPair(pkcs8Bytes: ByteArray): KeyPair {
        var keyFactory: KeyFactory
        var keySpec: PKCS8EncodedKeySpec
        return try {
            keyFactory = KeyFactory.getInstance("RSA")
            keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
            val privateKey = keyFactory.generatePrivate(keySpec) as RSAPrivateCrtKey
            val publicKeySpec = RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)
            KeyPair(keyFactory.generatePublic(publicKeySpec), privateKey)
        } catch (e: InvalidKeySpecException) {
            try {
                keyFactory = KeyFactory.getInstance("EC")
                keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                val privateKey = keyFactory.generatePrivate(keySpec) as ECPrivateKey
                val publicKeySpec = ECPublicKeySpec(privateKey.params.generator, privateKey.params)
                KeyPair(keyFactory.generatePublic(publicKeySpec), privateKey)
            } catch (ee: InvalidKeySpecException) {
                try {
                    keyFactory = KeyFactory.getInstance("EC")
                    keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                    val privateKey = keyFactory.generatePrivate(keySpec) as ECPrivateKey
                    val publicKeySpec = ECPublicKeySpec(privateKey.params.generator, privateKey.params)
                    KeyPair(keyFactory.generatePublic(publicKeySpec), privateKey)
                } catch (ee: InvalidKeySpecException) {
                    e.addSuppressed(ee)
                    throw e
                }
            }
        }
    }

    private fun readPkcs1PrivateKeyPair(pkcs1Bytes: ByteArray): KeyPair {
        val pkcs1Length = pkcs1Bytes.size
        val totalLength = pkcs1Length + 22
        val pkcs8Header =
            byteArrayOf(48,
                -126,
                (totalLength shr 8 and 255).toByte(),
                (totalLength and 255).toByte(),
                2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126,
                (pkcs1Length shr 8 and 255).toByte(),
                (pkcs1Length and 255).toByte())
        val pkcs8bytes = join(pkcs8Header, pkcs1Bytes)
        return readPkcs8KeyPair(pkcs8bytes)
    }


    /**
     * Load an RSA public key from bytes
     *
     * @param keyBytes key bytes
     * @return an RSA public key
     * @throws IOException file IO error
     * @throws GeneralSecurityException can't load private key
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun loadPublicKey(keyBytes: ByteArray): PublicKey {
        return loadKeyData(keyBytes, PUBLIC_KEY,
            { b -> readPkcs1PublicKey(b) },
            { b -> readPkcs8PublicKey(b) })
    }


    /**
     * Load an RSA public key from the given file.
     *
     * @param keyFile key file path
     * @return a RSA private key
     * @throws IOException file IO error
     * @throws GeneralSecurityException can't load private key
     */
    fun loadPublicKey(keyFile: File): PublicKey {
        return loadPublicKey(keyFile.readBytes())
    }

    /**
     * Load an RSA private key from bytes
     *
     * @param keyBytes key bytes
     * @return a RSA private key
     * @throws IOException file IO error
     * @throws GeneralSecurityException can't load private key
     */
    fun loadPrivateKey(keyBytes: ByteArray): PrivateKey {
        return loadKeyData(keyBytes,
            PRIVATE_KEY,
            { b -> readPkcs1PrivateKey(b) },
            { b -> readPkcs8PrivateKey(b) })
    }

    /**
     * Load an RSA private key from the given file.
     *
     * @param keyFile key file path
     * @return a RSA private key
     * @throws IOException file IO error
     * @throws GeneralSecurityException can't load private key
     */
    fun loadPrivateKey(keyFile: File): PrivateKey {
        return loadPrivateKey(keyFile.readBytes())
    }

    private fun readPkcs8PrivateKey(pkcs8Bytes: ByteArray): PrivateKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
        return keyFactory.generatePrivate(keySpec)
    }

    private fun readDsaPrivateKey(pkcs8Bytes: ByteArray): PrivateKey {
        val keyFactory = KeyFactory.getInstance("DSA")
        val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
        return keyFactory.generatePrivate(keySpec)
    }

    private fun readPkcs8PublicKey(pkcs8Bytes: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(pkcs8Bytes)
        return keyFactory.generatePublic(keySpec)
    }

    private fun readDsaPublicKey(pkcs8Bytes: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(pkcs8Bytes)
        return keyFactory.generatePublic(keySpec)
    }


    private fun readPkcs1PublicKey(pkcs1Bytes: ByteArray): PublicKey {
        return readPkcsKey(pkcs1Bytes) { b -> readPkcs8PublicKey(b) }
    }

    private fun readPkcs1PrivateKey(pkcs1Bytes: ByteArray): PrivateKey {
        return readPkcsKey(pkcs1Bytes) { b -> readPkcs8PrivateKey(b) }
    }

    private fun join(byteArray1: ByteArray, byteArray2: ByteArray): ByteArray {
        val bytes = ByteArray(byteArray1.size + byteArray2.size)
        System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.size)
        System.arraycopy(byteArray2, 0, bytes, byteArray1.size, byteArray2.size)
        return bytes
    }

    private fun <T> readPkcsKey(pkcs1Bytes: ByteArray, fn: (ByteArray) -> T): T {
        // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
        val pkcs1Length = pkcs1Bytes.size
        val totalLength = pkcs1Length + 22
        // reference to https://github.com/Mastercard/client-encryption-java/blob/master/src/main/java/com/mastercard/developer/utils/EncryptionUtils.java#L95-L100
        val pkcs8Header = byteArrayOf(
            0x30, 0x82.toByte(),
            (totalLength shr 8 and 0xff).toByte(), (totalLength and 0xff).toByte(),  // Sequence + total length
            0x2, 0x1, 0x0, 0x30, 0xD, 0x6, 0x9, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0xD, 0x1, 0x1, 0x1, 0x5, 0x0, 0x4, 0x82.toByte(),
            (pkcs1Length shr 8 and 0xff).toByte(),
            (pkcs1Length and 0xff).toByte()
        )
        val pkcs8bytes = join(pkcs8Header, pkcs1Bytes)
        return fn(pkcs8bytes)
    }


    fun createRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.genKeyPair()
    }

    fun createPemKeyFile(filename: String) {
        val file = File(filename)
        val pemWriter = JcaPEMWriter(FileWriter(file))
        pemWriter.use {
            pemWriter.writeObject(createRsaKeyPair().private)
        }
        constrainFilePermissions(file)
    }

    fun createPKCS8KeyFile(filename: String) {
        val file = File(filename)
        file.writeBytes(PKCS8EncodedKeySpec(createRsaKeyPair().private.encoded).encoded)
        constrainFilePermissions(file)
    }

    val RSAPrivateCrtKeyImpl.asPKCS8: ByteArray
        get() {
            val keyFactory = KeyFactory.getInstance(this.algorithm)
            val privateKeySpec = keyFactory.getKeySpec(this, PKCS8EncodedKeySpec::class.java)
            val keyBytes = privateKeySpec.encoded

            val s = PKCS_8_PEM_HEADER.replace("%", PRIVATE_KEY) + "\n" +
                    Base64.getMimeEncoder(64, System.lineSeparator().toByteArray()).encodeToString(keyBytes) +
                    "\n" + PKCS_8_PEM_FOOTER.replace("%", PRIVATE_KEY)
            return s.toByteArray()
        }


}