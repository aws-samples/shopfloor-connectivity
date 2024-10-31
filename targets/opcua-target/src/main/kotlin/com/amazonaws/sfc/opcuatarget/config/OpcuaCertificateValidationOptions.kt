
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.opcuatarget.config

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
    val keyUsageEndEntity: Boolean
        get() = _keyUsageEndEntity

    @SerializedName(CONFIG_EXT_KEY_USAGE_END)
    private var _extKeyUsageEndEntity: Boolean = true
    val extKeyUsageEndEntity: Boolean
        get() = _extKeyUsageEndEntity

    @SerializedName(CONFIG_REVOCATION)
    private var _revocation: Boolean = true
    val revocation: Boolean
        get() = _revocation

    @SerializedName(CONFIG_REVOCATION_LISTS)
    private var _revocationLists: Boolean = true
    val revocationLists: Boolean
        get() = _revocationLists

    @SerializedName(CONFIG_APPLICATION_URI)
    private var _applicationUri: Boolean = true
    val applicationUri: Boolean
        get() = _applicationUri


    val options: ImmutableSet<ValidationCheck>
        get() {
            val set = mutableSetOf<ValidationCheck>()
            if (hostOrIP) set.add(ValidationCheck.HOSTNAME)
            if (validity) set.add(ValidationCheck.VALIDITY)
            if (keyUsageEndEntity) set.add(ValidationCheck.KEY_USAGE_END_ENTITY)
            if (extKeyUsageEndEntity) set.add(ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY)
            if (revocation) set.add(ValidationCheck.REVOCATION)
            if (revocationLists) set.add(ValidationCheck.REVOCATION_LISTS)
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
            revocation: Boolean = default._revocation,
            revocationLists: Boolean = default._revocationLists,
            applicationUri: Boolean = default._applicationUri

        ): OpcuaCertificateValidationOptions {

            val instance = OpcuaCertificateValidationOptions()

            with(instance) {
                _hostOrIP = hostOrIP
                _validity = validity
                _keyUsageEndEntity = keyUsageEndEntityPresent
                _extKeyUsageEndEntity = extKeyUsageEndEntityPresent
                _revocation = revocation
                _revocationLists = revocationLists
                _applicationUri = applicationUri

            }
            return instance
        }


        const val CONFIG_HOST_OR_IP = "HostOrIP"
        const val CONFIG_VALIDITY = "Validity"
        const val CONFIG_KEY_USAGE_END = "KeyUsageEndEntity"
        const val CONFIG_EXT_KEY_USAGE_END = "ExtKeyUsageEndEntity"
        const val CONFIG_REVOCATION_LISTS = "RevocationLists"
        const val CONFIG_REVOCATION = "Revocation"
        const val CONFIG_APPLICATION_URI = "ApplicationUri"
    }

    override fun toString(): String {
        return "($CONFIG_HOST_OR_IP=$hostOrIP, $CONFIG_VALIDITY=$validity, $CONFIG_KEY_USAGE_END=$keyUsageEndEntity, $CONFIG_EXT_KEY_USAGE_END=$extKeyUsageEndEntity,  $CONFIG_REVOCATION=$revocation,  $CONFIG_REVOCATION_LISTS=$revocationLists, $CONFIG_APPLICATION_URI=$applicationUri)"
    }

}