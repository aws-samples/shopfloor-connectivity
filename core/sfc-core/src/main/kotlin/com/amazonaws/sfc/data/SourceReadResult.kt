/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.google.gson.Gson
import java.time.Instant

/**
 * Result from reading from single source
 * @property timestamp Instant timestamp at source level
 */

sealed class SourceReadResult(ts: Instant?) {
    val timestamp: Instant = ts ?: systemDateTime()
    val gson: Gson by lazy { JsonHelper.gsonPretty() }
}


/**
 * Read values from a successful read result from a source
 * @property values Mapping<String, ChannelReadValue> Values read from the source, map index by channel
 * @property timestamp Instant timestamp at source level
 */
class SourceReadSuccess(val values: Map<String, ChannelReadValue>, timestamp: Instant? = systemDateTime()) : SourceReadResult(timestamp) {

    /**
     * Read value success as a string
     * @return String
     */
    override fun toString(): String =
        gson.toJson(mapOf("timestamp" to timestamp.toString(), "values" to values.map { it.key to it.value.asMap() }.toMap()))
}


/**
 * Error reading from a single source
 * @property error String Read error message
 * @property timestamp Instant timestamp at source level
 */
class SourceReadError(val error: String, timestamp: Instant? = systemDateTime()) : SourceReadResult(timestamp) {
    /**
     * REad error as a string
     * @return String
     */
    override fun toString(): String = gson.toJson(mapOf("timestamp" to timestamp.toString(), "error" to error))
}