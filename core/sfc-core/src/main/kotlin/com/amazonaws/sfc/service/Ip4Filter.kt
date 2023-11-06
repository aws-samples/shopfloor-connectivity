
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

class Ip4Filter(private val allowedIps: List<String>?) {

    fun isIp4AddressAllowed(ipAddress: String): Boolean {
        if (allowedIps.isNullOrEmpty()) {
            // address is valid if list is null or empty
            return true
        }

        val ipParts = ipAddress.split(".")
        return allowedIps.any { allowedIp ->
            val allowedParts = allowedIp.split(".")
            allowedParts.indices.all { allowedParts[it] == "*" || allowedParts[it] == ipParts[it] }
        }
    }
}