
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import com.amazonaws.encryptionsdk.jce.JceMasterKey

interface MasterKey {
    val masterKey: JceMasterKey
}