/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */


package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.decodeString
import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.google.gson.Gson

class LibVersion(
    val major: UShort = 0.toUShort(),
    val minor: UShort = 0.toUShort(),
    val build: UShort = 0.toUShort(),
    val revision: UShort = 0.toUShort(),
    val flags: Int = 0,
    val version: String = ""
) {

    class LibVersionBuilder {

        private var major: UShort = 0.toUShort()
        private var minor: UShort = 0.toUShort()
        private var build: UShort = 0.toUShort()
        private var revision: UShort = 0.toUShort()
        private var flags: Int = 0
        private var version: String = ""

        fun major(major: UShort): LibVersionBuilder {
            this.major = major
            return this
        }

        fun minor(minor: UShort): LibVersionBuilder {
            this.minor = minor
            return this
        }

        fun build(build: UShort): LibVersionBuilder {
            this.build = build
            return this
        }

        fun revision(revision: UShort): LibVersionBuilder {
            this.revision = revision
            return this
        }

        fun flags(flags: Int): LibVersionBuilder {
            this.flags = flags
            return this
        }

        fun version(version: String): LibVersionBuilder {
            this.version = version
            return this
        }


        fun build() = LibVersion(major, minor, build, revision, flags, version)
    }

    companion object {

        private fun builder() = LibVersionBuilder()

        fun fromBytes(bytes: ByteArray): LibVersion {

            return builder()
                .major(bytes.toInt16(0).toUShort())
                .minor(bytes.toInt16(2).toUShort())
                .build(bytes.toInt16(4).toUShort())
                .revision(bytes.toInt16(6).toUShort())
                .flags(bytes.toInt32(8))
                .version(decodeString(bytes.sliceArray(12..<36)))
                .build()
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

}