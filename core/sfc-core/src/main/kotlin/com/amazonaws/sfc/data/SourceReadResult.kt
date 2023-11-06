
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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