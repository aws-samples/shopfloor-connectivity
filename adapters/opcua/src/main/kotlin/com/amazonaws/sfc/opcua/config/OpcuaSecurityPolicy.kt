
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua.config

import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy

enum class OpcuaSecurityPolicy(val policy: SecurityPolicy) {
    @SerializedName("None")
    None(SecurityPolicy.None),

    @SerializedName("Basic128Rsa15")
    Basic128Rsa15(SecurityPolicy.Basic128Rsa15),

    @SerializedName("Basic256")
    Basic256(SecurityPolicy.Basic256),

    @SerializedName("Basic256Sha256")
    Basic256Sha256(SecurityPolicy.Basic256Sha256),

    @SerializedName("Aes128Sha256RsaOaep")
    Aes128Sha256RsaOaep(SecurityPolicy.Aes128_Sha256_RsaOaep)
}