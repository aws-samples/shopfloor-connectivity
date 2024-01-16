/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.decodeDateTime
import com.amazonaws.sfc.ads.protocol.Decoder.decodeString
import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.google.gson.Gson

class PlcAppSystemInfo(
    val objID: UInt = 0.toUInt(),
    val taskCount: UInt = 0.toUInt(),
    val onlineChangeCount: UInt = 0.toUInt(),
    val flags: Short = 0,
    val adsPort: UShort = 0.toUShort(),
    val bootDataLoaded: Boolean = false,
    val oldBootData: Boolean = false,
    val appTimestamp: String = "",
    val keepOutputsOnBP: Boolean = false,
    val shutdownInProgress: Boolean = false,
    val licencesPending: Boolean = false,
    val bsodOccurred: Boolean = false,
    val loggedIn: Boolean = false,
    val appName: String = "",
    val projectName: String = ""
) {

    override fun toString(): String {
        return Gson().toJson(this)
    }

    class PlcAppSystemInfoBuilder {

        private var objId: UInt = 0.toUInt()
        private var taskCnt: UInt = 0.toUInt()
        private var onlineChangeCnt: UInt = 0.toUInt()
        private var flags: Short = 0
        private var adsPort: UShort = 0.toUShort()
        private var bootDataLoaded: Boolean = false
        private var oldBootData: Boolean = false
        private var appTimestamp: String = ""
        private var keepOutputsOnBP: Boolean = false
        private var shutdownInProgress: Boolean = false
        private var licencesPending: Boolean = false
        private var bsodOccurred: Boolean = false
        private var loggedIn: Boolean = false
        private var appName: String = ""
        private var projectName: String = ""

        fun objId(objId: UInt): PlcAppSystemInfoBuilder {
            this.objId = objId
            return this
        }

        fun taskCnt(taskCnt: UInt): PlcAppSystemInfoBuilder {
            this.taskCnt = taskCnt
            return this
        }

        fun onlineChangeCnt(onlineChangeCnt: UInt): PlcAppSystemInfoBuilder {
            this.onlineChangeCnt = onlineChangeCnt
            return this
        }

        fun flags(flags: Short): PlcAppSystemInfoBuilder {
            this.flags = flags
            return this
        }

        fun adsPort(adsPort: UShort): PlcAppSystemInfoBuilder {
            this.adsPort = adsPort
            return this
        }

        fun bootDataLoaded(bootDataLoaded: Boolean): PlcAppSystemInfoBuilder {
            this.bootDataLoaded = bootDataLoaded
            return this
        }

        fun oldBootData(oldBootData: Boolean): PlcAppSystemInfoBuilder {
            this.oldBootData = oldBootData
            return this
        }

        fun appTimestamp(appTimestamp: String): PlcAppSystemInfoBuilder {
            this.appTimestamp = appTimestamp
            return this
        }

        fun keepOutputsOnBP(keepOutputsOnBP: Boolean): PlcAppSystemInfoBuilder {
            this.keepOutputsOnBP = keepOutputsOnBP
            return this
        }

        fun shutdownInProgress(shutdownInProgress: Boolean): PlcAppSystemInfoBuilder {
            this.shutdownInProgress = shutdownInProgress
            return this
        }

        fun licencesPending(licencesPending: Boolean): PlcAppSystemInfoBuilder {
            this.licencesPending = licencesPending
            return this
        }

        fun bsodOccurred(bsodOccurred: Boolean): PlcAppSystemInfoBuilder {
            this.bsodOccurred = bsodOccurred
            return this
        }

        fun loggedIn(loggedIn: Boolean): PlcAppSystemInfoBuilder {
            this.loggedIn = loggedIn
            return this
        }


        fun appName(appName: String): PlcAppSystemInfoBuilder {
            this.appName = appName
            return this
        }

        fun projectName(projectName: String): PlcAppSystemInfoBuilder {
            this.projectName = projectName
            return this
        }

        fun build() = PlcAppSystemInfo(
            objId,
            taskCnt,
            onlineChangeCnt,
            flags,
            adsPort,
            bootDataLoaded,
            oldBootData,
            appTimestamp,
            keepOutputsOnBP,
            shutdownInProgress,
            licencesPending,
            bsodOccurred,
            loggedIn,
            appName,
            projectName
        )
    }

    companion object {

        private fun builder() = PlcAppSystemInfoBuilder()

        fun fromBytes(bytes: ByteArray): PlcAppSystemInfo {

            return builder()
                .objId(bytes.toInt32(0).toUInt())
                .taskCnt(bytes.toInt32(4).toUInt())
                .onlineChangeCnt(bytes.toInt32(8).toUInt())
                .flags(bytes.toInt16(12))
                .adsPort(bytes.toInt16(16).toUShort())
                .bootDataLoaded(bytes[18].toInt() == 1)
                .oldBootData(bytes[19].toInt() == 1)
                .appTimestamp(decodeDateTime(bytes.sliceArray(20..<24)))
                .keepOutputsOnBP(bytes[24].toInt() == 1)
                .shutdownInProgress(bytes[25].toInt() == 1)
                .licencesPending(bytes[26].toInt() == 1)
                .bsodOccurred(bytes[27].toInt() == 1)
                .loggedIn(bytes[28].toInt() == 1)
                .appName(decodeString(bytes.sliceArray(64..<128)))
                .projectName(decodeString(bytes.sliceArray(128..<192)))
                .build()
        }
    }

}