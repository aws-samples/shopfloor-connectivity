// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.util.asHexString


class SlmpError {

    companion object {

        fun errorString(endCode: Short): String {
            val s = when (endCode) {
                0x0000.toShort() -> "No error"
                0xC050.toShort() -> "Received ASCII code can not be converted to binary"
                0xC051.toShort() -> "Maximum number of bit devices for which data can be read/written all at once is outside the allowable range"
                0xC052.toShort() -> "Maximum number of word devices for which data can be read/written all at once is outside the allowable range"
                0xC053.toShort() -> "Maximum number of bit devices for which data can be random read/written all at once is outside the allowable range"
                0xC054.toShort() -> "Maximum number of word devices for which data can be random read/written all at once is outside the allowable range"
                0xC056.toShort() -> "Read or write request exceeds maximum address"
                0xC058.toShort() -> "Request data length after ASCII-to-binary conversion does not match the number of data in the character section (part of text)"
                0xC059.toShort() -> "Error in command or subcommand specification, there is a command or subcommand that cannot be used by he CPU module"
                0xC05B.toShort() -> "CPU module cannot read or write from/to specified device"
                0xC05C.toShort() -> "Error in request contents. (Reading or writing by bit unit for word device, etc.)"
                0xC05F.toShort() -> "Request that cannot be executed for the target CPU module"
                0xC060.toShort() -> "Error in request contents. (Error in specification of data for bit device, etc.)"
                0xC061.toShort() -> "Request data length does not match the number of data in the character section (part of text)"
                0xC06F.toShort() -> "When the communication data code is set to \"Binary\", a request message of ASCII is received. (Error history of this error code is registered but no error response is sent"
                0xC0D8.toShort() -> "The number of specified blocks exceeds the range"
                0xC200.toShort() -> "Error in remote password"
                0xC201.toShort() -> "Locked status of the remote password of the port which is used for communication"
                0xC204.toShort() -> "Different device requested remote password to be unlocked"
                0xC810.toShort() -> "Error in remote password. (Authentication failure count is 9 less)"
                0XC815.toShort() -> "Error in remote password. (Authentication failure count is 10)"
                0XC816.toShort() -> "Remote password authentication is locked out"
                in (0x4000..0x4FFF) -> "CPU error"
                else -> {
                    "Unknown or no SLMP error code"
                }
            }
            return "${endCode.asHexString()} : $s"
        }

        const val NO_ERROR = 0.toShort()


    }
}