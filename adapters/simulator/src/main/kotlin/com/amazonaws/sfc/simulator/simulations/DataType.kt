//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import kotlin.reflect.KClass

enum class DataType {
    BOOLEAN {
        override val kotlinClass = Boolean::class
    },
    BYTE {
        override val kotlinClass = Byte::class
    },
    SHORT {
        override val kotlinClass = Short::class
    },
    INT {
        override val kotlinClass = Int::class
    },
    LONG {
        override val kotlinClass = Long::class
    },
    FLOAT {
        override val kotlinClass = Float::class
    },
    DOUBLE {
        override val kotlinClass = Double::class
    },
    STRING {
        override val kotlinClass = String::class
    },
    CHAR {
        override val kotlinClass = Char::class
    },
    UBYTE {
        override val kotlinClass = UByte::class
    },
    USHORT {
        override val kotlinClass = UShort::class
    },
    UINT {
        override val kotlinClass = UInt::class
    },
    ULONG {
        override val kotlinClass = ULong::class
    },
    UNDEFINED {
        override val kotlinClass = Double::class
    };

    abstract val kotlinClass: KClass<*>

    companion object {
        fun fromString(s: String) =
            DataType.entries.find { it.name == s.uppercase() } ?: UNDEFINED
    }

}

