
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.mqtt.config

import com.google.gson.annotations.SerializedName

enum class MqttProtocol {

    //TODO add support for tls, ws, wss
    @SerializedName("tcp")
    TCP;

    override fun toString(): String {
        return when (TCP) {
            this -> "tcp"
            else -> "$this"
        }
    }

}

