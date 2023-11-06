
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.log

import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
class CustomLogWriter(private val configStr: String) : LogWriter {

    override fun write(logLevel: LogLevel, timestamp: Long, source: String?, message: String) {
        val dtm = "%-23s".format(SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SS").format(Date()))
        val sourceStr = if (source != null) "[$source] :" else ""
        println("$dtm $logLevel- $sourceStr $message")
    }

    override fun close() {
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any): CustomLogWriter {
            return CustomLogWriter(createParameters[0] as String)
        }

    }

}