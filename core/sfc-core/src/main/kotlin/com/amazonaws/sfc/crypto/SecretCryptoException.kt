
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

class SecretCryptoException : Exception {

    constructor (err: String?) : super(err)

    constructor(err: Exception?) : super(err)

    constructor(err: String?, e: java.lang.Exception?) : super(err, e)


}