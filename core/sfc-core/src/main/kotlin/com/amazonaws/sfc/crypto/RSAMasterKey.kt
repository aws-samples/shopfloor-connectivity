
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import com.amazonaws.encryptionsdk.jce.JceMasterKey
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey

class RSAMasterKey private constructor(publicKey: PublicKey,
                                       privateKey: PrivateKey) : MasterKey {

    private val _masterKey: JceMasterKey

    init {
        val keyId = publicKeySHA(publicKey)
        _masterKey = JceMasterKey.getInstance(publicKey, privateKey, KEY_PROVIDER, keyId, WRAPPING_ALGO)
    }

    private fun publicKeySHA(key: PublicKey): String {
        return try {
            val sha1 = MessageDigest.getInstance("SHA-1").digest(key.encoded)
            String(org.bouncycastle.util.encoders.Hex.encode(sha1))
        } catch (e: NoSuchAlgorithmException) {
            throw SecretCryptoException("Unable to get SHA-1 provider", e)
        }
    }

    override val masterKey: JceMasterKey
        get() = _masterKey

    companion object {
        // Metadata used by JCEMasterKey, so that another instance of JCEMasterKey cannot be used
        // to encrypt/decrypt payload even if it uses the same key.
        private const val KEY_PROVIDER = "sfc:secrets"
        private const val WRAPPING_ALGO = "RSA/ECB/PKCS1Padding"
        fun createInstance(publicKey: PublicKey,
                           privateKey: PrivateKey): MasterKey {
            return RSAMasterKey(publicKey, privateKey)
        }
    }

}
