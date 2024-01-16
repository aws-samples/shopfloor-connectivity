/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.google.gson.Gson

class Version(
    val major: UShort = 0.toUShort(),
    val minor: UShort = 0.toUShort(),
    val servicePack: UShort = 0.toUShort(),
    val patch: UShort = 0.toUShort(),
) {

    class LibVersionBuilder {

        private var major: UShort = 0.toUShort()
        private var minor: UShort = 0.toUShort()
        private var servicePack: UShort = 0.toUShort()
        private var patch: UShort = 0.toUShort()

        fun major(major: UShort): LibVersionBuilder {
            this.major = major
            return this
        }

        fun minor(minor: UShort): LibVersionBuilder {
            this.minor = minor
            return this
        }

        fun servicePack(servicePack: UShort): LibVersionBuilder {
            this.servicePack = servicePack
            return this
        }

        fun patch(patch: UShort): LibVersionBuilder {
            this.patch = patch
            return this
        }


        fun build() = Version(major, minor, servicePack, patch)
    }

    companion object {

        private fun builder() = LibVersionBuilder()

        fun fromBytes(bytes: ByteArray): Version {

            return builder()
            .major(bytes.toInt16(0).toUShort())
            .minor(bytes.toInt16(2).toUShort())
            .servicePack(bytes.toInt16(4).toUShort())
            .patch(bytes.toInt16(6).toUShort())
            .build()
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

}