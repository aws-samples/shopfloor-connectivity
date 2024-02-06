// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.log

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_LOG_LEVEL
import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.service.CommandLine.Companion.OPTION_LOGLEVEL_ERROR
import com.amazonaws.sfc.service.CommandLine.Companion.OPTION_LOGLEVEL_INFO
import com.amazonaws.sfc.service.CommandLine.Companion.OPTION_LOGLEVEL_TRACE
import com.amazonaws.sfc.service.CommandLine.Companion.OPTION_LOGLEVEL_WARNING
import com.google.gson.annotations.SerializedName


/**
 * Log levels
 */
enum class LogLevel {

    /**
     * Always on for error messages
     */
    @SerializedName("Error")
    ERROR,

    /**
     * Warning + Error messages
     */
    @SerializedName("Warning")
    WARNING,

    /**
     * Warning + Error + Informational messages
     */
    @SerializedName("Info")
    INFO,

    /**
     * TRace + Warning + Error + Informational messages
     */
    @SerializedName("Trace")
    TRACE;

    companion object {

        fun fromArgs(args: Array<String>): LogLevel? {
            val a = args.map { it.trimStart('-') }
            return when {
                a.contains(OPTION_LOGLEVEL_TRACE) -> TRACE
                a.contains(OPTION_LOGLEVEL_WARNING) -> WARNING
                a.contains(OPTION_LOGLEVEL_ERROR) -> ERROR
                a.contains(OPTION_LOGLEVEL_INFO) -> INFO
                else -> null
            }
        }

        fun fromConfigString(s: String): LogLevel? =
            try{
                val configRaw = JsonHelper.Companion.fromJsonExtended(s, Any::class.java) as Map<*, *>
                if (configRaw[CONFIG_LOG_LEVEL] != null) fromArgs(arrayOf(configRaw[CONFIG_LOG_LEVEL] as String)) else null
            } catch (e: Exception) {
                null
            }

    }
}