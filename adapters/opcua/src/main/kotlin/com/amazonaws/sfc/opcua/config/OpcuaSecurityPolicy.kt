/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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