
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import java.security.KeyPair

interface CryptoKeySpi {
    //fun getKeyManagers(privateKeyUri: URI): Array<KeyManager>
    //    fun getKeyPair(privateKeyUri: URI): KeyPair
    //    fun getKeyPair(privateKeyBytes : ByteArray) : KeyPair
    fun getKeyPair(privateKey: KeyContainer): KeyPair
    val supportedKeyType: String
}