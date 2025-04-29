// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.targets

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger

abstract class TargetFormatter(val configuration: String, val logger : Logger) {

    open fun itemPayloadSize(targetData : TargetData) : Int  { return 0}

    abstract fun  apply( targetData : List<TargetData>) : ByteArray

    fun apply(targetData : TargetData) : ByteArray {
        return apply(listOf(targetData))
    }
}

