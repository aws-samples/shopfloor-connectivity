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

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.jce.JceMasterKey
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory
import java.util.*

class Crypter(keyChain: KeyChain) {
    private val keyChain: KeyChain

    init {
        if (keyChain.masterKey == null) {
            throw SecretCryptoException("Empty Key chain provided")
        }
        this.keyChain = keyChain
    }

    /**
     * Decrypt a cipher text using installed keychain.
     * @param payload cipher text to be decrypted.
     * @param context additional context to be matched with the context stored with encrypted data.
     * @return decrypted plain text.
     * @throws SecretCryptoException when inputs are invalid, or context mismatches or decryption fails.
     */
    fun decrypt(payload: ByteArray, context: String?): ByteArray {
        return try {
            if (context.isNullOrEmpty()) {
                throw SecretCryptoException("Invalid context provided")
            }
            if (payload.isEmpty()) {
                throw SecretCryptoException("Empty input payload provided")
            }
            val keyList: List<JceMasterKey> = keyChain.keyProviders
            val awsCrypto = AwsCrypto.builder().build()
            val provider = MultipleProviderFactory.buildMultiProvider(keyList)
            val decryptResult: CryptoResult<ByteArray, *> = awsCrypto.decryptData(provider, payload)
            val decryptedPayload = decryptResult.result
            val decryptedPayloadContext = decryptResult.encryptionContext
            if (context != decryptedPayloadContext[CONTEXT_STR]) {
                throw SecretCryptoException(String.format("Context mismatch, expected %s, but found %s",
                    context,
                    decryptedPayloadContext))
            }
            decryptedPayload
        } catch (e: Exception) {
            throw SecretCryptoException(e)
        }
    }

    /**
     * Encrypt a plain text using installed keychain.
     * @param plainText text to be encrypted
     * @param context Additional context stored with encrypted text.
     * @return cipher text with additional context as provided.
     * @throws SecretCryptoException if parameters are invalid or if there is any issue during encryption.
     */
    @Throws(SecretCryptoException::class) fun encrypt(plainText: ByteArray, context: String?): ByteArray {
        return try {
            if (context.isNullOrEmpty()) {
                throw SecretCryptoException("Invalid context provided")
            }
            if (plainText.isEmpty()) {
                throw SecretCryptoException("Empty input plainText provided")
            }
            // masterKey cannot be null as keychain is added only and empty keychain is not allowed.
            val masterKey: JceMasterKey = keyChain.masterKey ?: throw java.lang.Exception("No master key in keychain")
            val awsCrypto = AwsCrypto.builder().build()
            val encryptionContext = Collections.singletonMap(CONTEXT_STR, context)
            val encryptionResult: CryptoResult<ByteArray, *> = awsCrypto.encryptData(masterKey, plainText, encryptionContext)
            encryptionResult.result
        } catch (e: Exception) {
            throw SecretCryptoException(e)
        }
    }

    companion object {
        private const val CONTEXT_STR = "Context"
    }
}
