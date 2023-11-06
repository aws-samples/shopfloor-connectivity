
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

/**
 * Exception raised for errors when loading keys by the SecurityService
 */
class KeyLoadingException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, e: Throwable?) : super(message, e)
}