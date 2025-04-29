// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class J1939AdapterSocketConfiguration : Validate {

    @SerializedName(value = CONFIG_SOCKET_NAME)
    private var _socketName: String = ""

    val socketName: String
        get() = _socketName

    private var _validated = false
    override var validated
        get() = _validated

        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            socketName.isNotEmpty(),
            "$CONFIG_SOCKET_NAME is required for J1939AdapterSocketConfiguration",
            CONFIG_SOCKET_NAME,
            this
        )


        validated = true
    }


    companion object {

        private const val CONFIG_SOCKET_NAME = "SocketName"

        private val default = J1939AdapterSocketConfiguration()

        fun create(socketName : String = default._socketName): J1939AdapterSocketConfiguration {

            val instance = J1939AdapterSocketConfiguration()

            with(instance) {
                _socketName = socketName
            }
            return instance
        }

    }
}








