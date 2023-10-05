/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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