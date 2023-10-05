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