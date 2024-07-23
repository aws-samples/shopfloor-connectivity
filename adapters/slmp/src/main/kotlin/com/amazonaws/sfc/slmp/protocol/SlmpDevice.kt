// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

enum class SlmpDevice{


    SPECIAL_RELAY {
        override val deviceCode: String = "SM"
        override val device: Short = 0x91
        override val deviceType: DeviceType = DeviceType.BIT
    },

    SPECIAL_REGISTER {
        override val deviceCode: String = "SD"
        override val device: Short = 0XA9
        override val deviceType: DeviceType = DeviceType.WORD
    },

    INPUT {
        override val deviceCode: String = "X"
        override val device: Short = 0X9C
        override val deviceType: DeviceType = DeviceType.BIT
    },

    OUTPUT {
        override val deviceCode: String = "Y"
        override val device: Short = 0X9D
        override val deviceType: DeviceType = DeviceType.BIT
    },

    INTERNAL_RELAY {
        override val deviceCode: String = "M"
        override val device: Short = 0X90
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LATCHING_RELAY {
        override val deviceCode: String = "L"
        override val device: Short = 0X92
        override val deviceType: DeviceType = DeviceType.BIT
    },

    ALARM {
        override val deviceCode: String = "F"
        override val device: Short = 0X93
        override val deviceType: DeviceType = DeviceType.BIT
    },

    EDGE_RELAY {
        override val deviceCode: String = "V"
        override val device: Short = 0X94
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LINK_RELAY {
        override val deviceCode: String = "B"
        override val device: Short = 0XA0
        override val deviceType: DeviceType = DeviceType.BIT
    },

    STEP_RELAY{
        override val deviceCode: String = "S"
        override val device: Short = 0X98
        override val deviceType: DeviceType = DeviceType.BIT
    },

    DATA_REGISTER {
        override val deviceCode: String = "D"
        override val device: Short = 0xA8
        override val deviceType: DeviceType = DeviceType.WORD
    },

    LINK_REGISTER {
        override val deviceCode: String = "W"
        override val device: Short = 0xB4
        override val deviceType: DeviceType = DeviceType.WORD
    },

    TIMER_CONTACT{
        override val deviceCode: String = "TS"
        override val device: Short = 0XC1
        override val deviceType: DeviceType = DeviceType.BIT
    },

    TIMER_COIL {
        override val deviceCode: String = "TC"
        override val device: Short = 0xC0
        override val deviceType: DeviceType = DeviceType.BIT
    },

    TIMER_CURRENT_VALUE {
        override val deviceCode: String = "TN"
        override val device: Short = 0xC2
        override val deviceType: DeviceType = DeviceType.WORD
    },

    LONG_TIMER_CONTACT {
        override val deviceCode: String = "LTS"
        override val device: Short = 0X51
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_TIMER_COIL {
        override val deviceCode: String = "LTC"
        override val device: Short = 0X50
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_TIMER_CURRENT_VALUE {
        override val deviceCode: String = "LTN"
        override val device: Short = 0X52
        override val deviceType: DeviceType = DeviceType.DOUBLEWORD
    },

    LONG_RETENTIVE_TIMER_CONTACT {
        override val deviceCode: String = "LSTS"
        override val device: Short = 0X59
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_RETENTIVE_TIMER_COIL {
        override val deviceCode: String = "LSTC"
        override val device: Short = 0X58
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_RETENTIVE_TIMER_CURRENT_VALUE {
        override val deviceCode: String = "LSTN"
        override val device: Short = 0X5A
        override val deviceType: DeviceType = DeviceType.DOUBLEWORD
    },

   COUNTER_CONTACT {
        override val deviceCode: String = "CS"
        override val device: Short = 0XC4
        override val deviceType: DeviceType = DeviceType.BIT
    },

    COUNTER_COIL {
        override val deviceCode: String = "CC"
        override val device: Short = 0XC3
        override val deviceType: DeviceType = DeviceType.BIT
    },

    COUNTER_TIMER_CURRENT_VALUE {
        override val deviceCode: String = "CN"
        override val device: Short = 0XC5
        override val deviceType: DeviceType = DeviceType.WORD
    },

    LONG_COUNTER_CONTACT {
        override val deviceCode: String = "LCS"
        override val device: Short = 0X55
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_COUNTER_COIL {
        override val deviceCode: String = "LCC"
        override val device: Short = 0X54
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LONG_COUNTER_TIMER_CURRENT_VALUE {
        override val deviceCode: String = "LCN"
        override val device: Short = 0X56
        override val deviceType: DeviceType = DeviceType.DOUBLEWORD
    },

    LINK_SPECIAL_RELAY{
        override val deviceCode: String = "SB"
        override val device: Short = 0XA1
        override val deviceType: DeviceType = DeviceType.BIT
    },

    LINK_SPECIAL_REGISTER{
        override val deviceCode: String = "SW"
        override val device: Short = 0XB5
        override val deviceType: DeviceType = DeviceType.WORD
    },

    DIRECT_ACCESS_INPUT {
        override val deviceCode: String = "DX"
        override val device: Short = 0XA2
        override val deviceType: DeviceType = DeviceType.BIT
    },

    DIRECT_ACCESS_OUTPUT {
        override val deviceCode: String = "DY"
        override val device: Short = 0XA3
        override val deviceType: DeviceType = DeviceType.BIT
    },

    INDEX_REGISTER {
        override val deviceCode: String = "Z"
        override val device: Short = 0XCC
        override val deviceType: DeviceType = DeviceType.WORD
    },

    LONG_INDEX_REGISTER {
        override val deviceCode: String = "LZ"
        override val device: Short = 0X62
        override val deviceType: DeviceType = DeviceType.DOUBLEWORD
    },

    FILE_REGISTER_R {
        override val deviceCode: String = "R"
        override val device: Short = 0xAF
        override val deviceType: DeviceType = DeviceType.WORD
    },

    FILE_REGISTER_ZR {
        override val deviceCode: String = "ZR"
        override val device: Short = 0XB0
        override val deviceType: DeviceType = DeviceType.WORD
    },

    NULL_WORD_DEVICE {
        override val deviceCode: String = ""
        override val device: Short = 0x00
        override val deviceType: DeviceType = DeviceType.WORD
    },

    NULL_DOUBLEWORD_DEVICE {
        override val deviceCode: String = ""
        override val device: Short = 0x00
        override val deviceType: DeviceType = DeviceType.DOUBLEWORD
    };

    abstract val device: Short
    abstract val deviceCode: String
    abstract val deviceType: DeviceType

    val deviceCodeBytes : ByteArray
        get() = byteArrayOf(0x00, device.toByte())

    enum class DeviceType {
        WORD,
        BIT,
        DOUBLEWORD
    }


    companion object{
        fun fromString(s: String) : SlmpDevice?{
            return SlmpDevice.entries.firstOrNull{it.deviceCode == s.uppercase()}
        }

        private const val SUB_COMMAND_WORD: UShort = 0x0000U
        private const val SUB_COMMAND_BIT: UShort = 0x0001U
        private const val SUB_COMMAND_DOUBLE_WORD: UShort = 0x0002U

        val SlmpDevice.subCommand: UShort
            get() = when (this.deviceType) {
                DeviceType.WORD -> SUB_COMMAND_WORD
                DeviceType.BIT -> SUB_COMMAND_BIT
                DeviceType.DOUBLEWORD -> SUB_COMMAND_DOUBLE_WORD

            }
    }
}