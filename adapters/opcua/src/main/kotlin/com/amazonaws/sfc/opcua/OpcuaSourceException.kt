
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua

class OpcuaSourceException(private val sourceID: String, message: String) : Exception(message) {
    override fun toString(): String {
        return "OpcuaSourceException(sourceID=\"$sourceID\", $message)"
    }
}
