/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.   
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.common.collect.ImmutableSet
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck

@ConfigurationClass
class OpcuaCertificateValidationOptions {
    @SerializedName(CONFIG_HOST_OR_IP)
    private var _hostOrIP: Boolean = true
    val hostOrIP: Boolean
        get() = _hostOrIP

    @SerializedName(CONFIG_VALIDITY)
    private var _validity: Boolean = true
    val validity: Boolean
        get() = _validity

    @SerializedName(CONFIG_KEY_USAGE_END)
    private var _keyUsageEndEntity: Boolean = true
    val keyUsageEndEntit: Boolean
        get() = _keyUsageEndEntity

    @SerializedName(CONFIG_EXT_KEY_USAGE_END)
    private var _extKeyUsageEndEntity: Boolean = true
    val extKeyUsageEndEntity: Boolean
        get() = _extKeyUsageEndEntity

    @SerializedName(CONFIG_KEY_USAGE_ISSUER)
    private var _keyUsageIssuer: Boolean = true
    val keyUsageIssuer: Boolean
        get() = _keyUsageIssuer

    @SerializedName(CONFIG_REVOCATION)
    private var _revocation: Boolean = true
    val revocation: Boolean
        get() = _revocation

    @SerializedName(CONFIG_APPLICATION_URI)
    private var _applicationUri: Boolean = true
    val applicationUri: Boolean
        get() = _applicationUri


    val options: ImmutableSet<ValidationCheck>
        get() {
            val set = mutableSetOf<ValidationCheck>()
            if (hostOrIP) set.add(ValidationCheck.HOSTNAME)
            if (validity) set.add(ValidationCheck.VALIDITY)
            if (keyUsageEndEntit) set.add(ValidationCheck.KEY_USAGE_END_ENTITY)
            if (extKeyUsageEndEntity) set.add(ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY)
            if (keyUsageIssuer) set.add(ValidationCheck.KEY_USAGE_ISSUER)
            if (revocation) set.add(ValidationCheck.REVOCATION)
            if (applicationUri) set.add(ValidationCheck.APPLICATION_URI)
            return ImmutableSet.copyOf(set)
        }


    companion object {

        private val default = OpcuaCertificateValidationOptions()

        fun create(
            hostOrIP: Boolean = default._hostOrIP,
            validity: Boolean = default._validity,
            keyUsageEndEntityPresent: Boolean = default._keyUsageEndEntity,
            extKeyUsageEndEntityPresent: Boolean = default._extKeyUsageEndEntity,
            keyUsageIssuerPresent: Boolean = default._keyUsageIssuer,
            revocation: Boolean = default._revocation,
            applicationUri: Boolean = default._applicationUri

        ): OpcuaCertificateValidationOptions {

            val instance = OpcuaCertificateValidationOptions()

            with(instance) {
                _hostOrIP = hostOrIP
                _validity = validity
                _keyUsageEndEntity = keyUsageEndEntityPresent
                _extKeyUsageEndEntity = extKeyUsageEndEntityPresent
                _keyUsageIssuer = keyUsageIssuerPresent
                _revocation = revocation
                _keyUsageIssuer = keyUsageIssuerPresent
                _applicationUri = applicationUri

            }
            return instance
        }


        const val CONFIG_HOST_OR_IP = "HostOrIP"
        const val CONFIG_VALIDITY = "Validity"
        const val CONFIG_KEY_USAGE_END = "KeyUsageEndEntity"
        const val CONFIG_EXT_KEY_USAGE_END = "ExtKeyUsageEndEntity"
        const val CONFIG_KEY_USAGE_ISSUER = "KeyUsageIssuer"
        const val CONFIG_REVOCATION = "Revocation"
        const val CONFIG_APPLICATION_URI = "ApplicationUri"
    }

    override fun toString(): String {
        return "($CONFIG_HOST_OR_IP=$hostOrIP, $CONFIG_VALIDITY=$validity, $CONFIG_KEY_USAGE_END=$keyUsageEndEntit, $CONFIG_EXT_KEY_USAGE_END=$extKeyUsageEndEntity, $CONFIG_KEY_USAGE_ISSUER=$keyUsageIssuer, $CONFIG_REVOCATION=$revocation, $CONFIG_APPLICATION_URI=$applicationUri)"
    }

}