/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.system.DateTime.systemDateTime
import java.time.Instant

class ReadResult(val value: Any? = null,
                 val result: Int = 0,
                 val timestamp : Instant = systemDateTime(),
                 val symbol: Symbol) {
    override fun toString(): String {
        return "${symbol.symbolName}:${symbol.dataType}${symbol.arrayDimensionsStr} = $value" +
                (if (symbol.arrayDimensions != null) "" else "") +
                "(${if (value != null)value::class.simpleName else "null"}), " +
                "result = $result${if (result!=0) adsErrorString(result) else ""}  , " +
                "timestamp = $timestamp"
    }
}