/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.log.Logger
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.util.concurrent.ConcurrentHashMap


class SecurityService(private val privateKey: KeyContainer, private val logger: Logger) {

    private val className = this::class.java.simpleName

    private val cryptoKeyProviderMap = ConcurrentHashMap<String, CryptoKeySpi>()

    init {
        val defaultProvider = DefaultCryptoKeyProvider
        try {
            this.registerCryptoKeyProvider(defaultProvider)
        } catch (e: ServiceProviderException) {
            throw RuntimeException("Default provider has been registered", e)
        }
    }

    fun getKeyPair(): KeyPair {
        logger.getCtxTraceLog(className, "getKeyPair")("Get keypair from  $privateKey")
        val provider: CryptoKeySpi = selectCryptoKeyProvider(privateKey)
        return provider.getKeyPair(privateKey)
    }


    private fun registerCryptoKeyProvider(keyProvider: CryptoKeySpi) {
        val keyType = keyProvider.supportedKeyType

        if (cryptoKeyProviderMap.computeIfAbsent(keyType) { keyProvider } != keyProvider) {
            throw ServiceProviderException("Key type $keyType crypto key provider is registered")
        }
    }

    private fun selectCryptoKeyProvider(privateKey: KeyContainer): CryptoKeySpi {
        val scheme = privateKey.scheme
        return this.cryptoKeyProviderMap[privateKey.scheme]
               ?: throw CryptoServiceUnavailableException("Crypto key service for $scheme is unavailable")
    }

    object DefaultCryptoKeyProvider : CryptoKeySpi {

        override fun getKeyPair(privateKey: KeyContainer): KeyPair {
            return if (!isUriSupportedKeyType(privateKey.scheme)) {
                throw KeyLoadingException("Private key type \"${privateKey.scheme}\" is not supported, only supported type is \"$supportedKeyType\"")
            } else {
                try {
                    KeyHelpers.loadKeyPkcsKeyPair(privateKey.keyBytes)
                } catch (e: IOException) {
                    throw KeyLoadingException("Failed to get keypair", e)
                } catch (e: GeneralSecurityException) {
                    throw KeyLoadingException("Failed to get keypair", e)
                }
            }
        }

        const val KEY_TYPE_FILE = "file"
        const val SUPPORT_KEY_TYPE = KEY_TYPE_FILE

        override val supportedKeyType: String = SUPPORT_KEY_TYPE

        private fun isUriSupportedKeyType(scheme: String): Boolean = supportedKeyType == scheme

    }


}