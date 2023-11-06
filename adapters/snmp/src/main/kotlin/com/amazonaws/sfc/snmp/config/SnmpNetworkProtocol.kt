
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.snmp.config

import com.google.gson.annotations.SerializedName

enum class SnmpNetworkProtocol {


    @SerializedName("tcp")
    TCP,

    @SerializedName("udp")
    UDP;


    override fun toString(): String {
        return when {
            this == TCP -> "tcp"
            this == UDP -> "udp"
            else -> "$this"

        }
    }

}

