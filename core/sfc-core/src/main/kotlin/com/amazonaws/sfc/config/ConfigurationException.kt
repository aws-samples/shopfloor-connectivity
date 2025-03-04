
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended

interface Validate {
    @Throws(ConfigurationException::class)
    fun validate()
    var validated: Boolean
}

/**
 * Exception raised when validation detects invalid configuration data
 */
class ConfigurationException(message: String, val field: String, private val faultItem: Any? = null) : Exception(message) {
    override fun toString(): String {

        val f = if (faultItem != null)
            try {
                if (faultItem !is String)
                    gsonExtended().toJson(faultItem)
                else
                    faultItem
            } catch (e: Throwable) {
                faultItem
            }
        else
            "null"

        return "$message, location = \"$field\" ${if (f != null) ",item = $f" else ""}"
    }

    companion object {

        // Helper function to check and throw exception
        fun check(checkedCondition: Boolean, message: String, field: String, faultItem: Any? = null) {
            if (!checkedCondition) {
                val item = try {
                    if (faultItem == null) "" else JsonHelper.gsonPretty().toJson(faultItem)
                } catch (_: Exception) {
                    faultItem.toString()
                }
                throw ConfigurationException(message, field, item)
            }
        }

    }

}