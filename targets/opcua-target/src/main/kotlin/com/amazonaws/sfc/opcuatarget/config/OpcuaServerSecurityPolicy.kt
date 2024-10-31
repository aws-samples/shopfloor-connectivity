
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy

enum class OpcuaServerSecurityPolicy(val policy: SecurityPolicy) {
    @SerializedName(POLICY_NONE)
    None(SecurityPolicy.None),

    @SerializedName(POLICY_BASIC128RSA15)
    Basic128Rsa15(SecurityPolicy.Basic128Rsa15),

    @SerializedName(POLICY_BASIC256)
    Basic256(SecurityPolicy.Basic256),

    @SerializedName(POLICY_BASIC256SHA256)
    Basic256Sha256(SecurityPolicy.Basic256Sha256),

    @SerializedName(POLICY_AES128SHA256RSAOAEP)
    Aes128Sha256RsaOaep(SecurityPolicy.Aes128_Sha256_RsaOaep);


    companion object{
        private val ALL_POLICIES = setOf(None, Basic128Rsa15, Basic256, Basic256Sha256, Aes128Sha256RsaOaep)
        val VALID_POLICIES = ALL_POLICIES.map { it.policy }

        private const val POLICY_NONE = "None"
        private const val POLICY_BASIC128RSA15 = "Basic128Rsa15"
        private const val POLICY_BASIC256 = "Basic256"
        private const val POLICY_BASIC256SHA256 = "Basic256Sha256"
        private const val POLICY_AES128SHA256RSAOAEP = "Aes128Sha256RsaOaep"

        fun fromString( s : String) : OpcuaServerSecurityPolicy? {
            return when(s.trim()){
                POLICY_NONE -> None
                POLICY_BASIC128RSA15 -> Basic128Rsa15
                POLICY_BASIC256 -> Basic256
                POLICY_BASIC256SHA256 -> Basic256Sha256
                POLICY_AES128SHA256RSAOAEP -> Aes128Sha256RsaOaep
                else -> null
            }
        }
    }
}