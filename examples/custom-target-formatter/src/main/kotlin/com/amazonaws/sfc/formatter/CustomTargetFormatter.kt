// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.formatter

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.targets.TargetFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Suppress("unused")
class CustomTargetFormatter(configuration: String, logger : Logger) : TargetFormatter(configuration, logger) {

    override fun itemPayloadSize(targetData: TargetData) : Int{
        return targetData.toString().toByteArray().size
    }

    override fun apply(targetData: List<TargetData>): ByteArray {
        return targetData.map { target ->
            val date = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormat.format(date)
            val targetString = target.toString()
            val targetStringWithDate = "$formattedDate $targetString"
            targetStringWithDate.toByteArray()
        }.reduce { acc, bytes -> acc + bytes }
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): CustomTargetFormatter {
            @Suppress("UNCHECKED_CAST")
            return CustomTargetFormatter(createParameters[0] as String, createParameters[1] as Logger)
        }

    }

}