
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import com.amazonaws.encryptionsdk.jce.JceMasterKey

class KeyChain {

    private val _keyProviders: MutableList<JceMasterKey> = ArrayList()

    val keyProviders: List<JceMasterKey>
        get() = _keyProviders

    /**
     * Add a master key to this keychain.
     * @param masterKey master key to be added.
     */
    fun addMasterKey(masterKey: MasterKey) = _keyProviders.add(masterKey.masterKey)


    /**
     * Return the first master key in chain. This is useful for aws encryption SDK as it can be
     * encrypted using any key from the chain, as decrypt requires the whole keychain and would
     * decrypt as long as any key is able to decrypt the payload.
     * @return First master key in the chain.
     */
    val masterKey: JceMasterKey?
        get() = if (keyProviders.isEmpty()) null else keyProviders.first()
}