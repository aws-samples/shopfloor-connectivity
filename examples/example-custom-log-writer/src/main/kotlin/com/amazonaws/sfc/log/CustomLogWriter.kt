/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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