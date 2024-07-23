// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.slmp.protocol

class SlmpAccessPoint(val device: SlmpDevice, val deviceNumber: UShort){

    val deviceType = device.deviceType

    companion object {

        fun fromString(s: String): SlmpAccessPoint {
            val match = DEVICE_CODE_WITH_DEVICE_NUMBER_REGEX.matchEntire(s.trim().uppercase())
                ?: throw Exception("\"$s\" is not a valid access point")
            val deviceStr = match.groups[1]?.value?.uppercase() ?: ""
            val device = SlmpDevice.fromString(deviceStr)
                ?: throw throw SlmpException("\"$deviceStr\" is not a valid device code  in \"$s\", valid devices are ${SlmpDevice.entries
                    .filter { it.deviceCode.isNotBlank() }
                    .joinToString { it.deviceCode }}")

            val deviceNumberStr = match.groups[2]?.value
            val deviceNumber = deviceNumberStr?.toIntOrNull()
                ?: throw throw SlmpException("\"$deviceNumberStr\" is not a valid device number in \"$s\"")

            return SlmpAccessPoint(device, deviceNumber.toUShort())
        }

        private val DEVICE_CODE_WITH_DEVICE_NUMBER_REGEX = "([A-Z]+)(\\d+)".toRegex()
    }

    override fun toString(): String {
        return "SlmpAccessPoint(device=$device, deviceNumber=$deviceNumber)"
    }

}