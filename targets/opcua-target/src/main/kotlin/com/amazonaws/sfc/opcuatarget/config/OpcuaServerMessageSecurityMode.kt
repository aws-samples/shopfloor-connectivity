// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode

enum class OpcuaServerMessageSecurityMode(val mode : MessageSecurityMode) {
    @SerializedName(MODE_NONE)
    NONE(mode = MessageSecurityMode.None),
    @SerializedName(MODE_SIGN)
    SIGN(mode = MessageSecurityMode.Sign),
    @SerializedName(MODE_SIGN_AND_ENCRYPT)
    SIGN_AND_ENCRYPT(mode = MessageSecurityMode.SignAndEncrypt);


    companion object{
        private val ALL_SECURITY_MODES = setOf(NONE, SIGN, SIGN_AND_ENCRYPT)
        val VALID_SECURITY_MODES = ALL_SECURITY_MODES.map { it.mode }

        private const val MODE_NONE = "None"
        private const val MODE_SIGN = "Sign"
        private const val MODE_SIGN_AND_ENCRYPT = "SignAndEncrypt"

        fun fromString( s : String) : OpcuaServerMessageSecurityMode? {
            return when(s.trim()){
                MODE_NONE -> NONE
                MODE_SIGN -> SIGN
                MODE_SIGN_AND_ENCRYPT -> SIGN_AND_ENCRYPT
                else -> null
            }
        }
    }
}