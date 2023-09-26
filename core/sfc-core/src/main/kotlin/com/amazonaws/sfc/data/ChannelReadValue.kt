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

import java.time.Instant
import kotlin.reflect.KClass

/**
 * A data value, read from a source, with optional timestamp for the value
 */

class ChannelReadValue(value: Any?, val timestamp: Instant? = null) {

    /**
     * Value as a string
     * @return String
     */
    override fun toString(): String =
        asMap().toString()

    /**
     * Value as a map including value, data type and optional timestamp
     * @return Mapping<String, Any?>
     */
    fun asMap(): Map<String, Any?> {

        val map = if (!this.isArrayValue)
            mutableMapOf("value" to value, "type" to typeStr(value))
        else
            mutableMapOf<String, Any?>("value" to (value as List<*>).map { (ChannelReadValue(it)).asMap() }, "type" to typeStr(value))


        if (timestamp != null) {
            map["timestamp"] = timestamp
        }
        return map
    }

    // internal stored value
    private val _value = if (value is Array<*>) arrayListOf(*value) else value

    /**
     * The value
     */
    val value
        get() = _value

    /**
     * Returns true if value is an array
     */
    val isArrayValue: Boolean
        get() = _value is List<*>

    /**
     * Returns true if value is an array of ReadValues
     */
    val isNestedArrayValue: Boolean
        get() {
            if (!isArrayValue) return false

            val list = (_value as List<*>)

            return list.isNotEmpty() && (list.first() is ChannelReadValue)
        }

    /**
     * Returns type of the value
     */
    val dataType: KClass<*>?
        get() {
            if (isArrayValue) {
                val l = (value as Iterable<*>).toList()
                return if ((l.isEmpty()) || (l.first() == null)) null else l.first()!!::class
            }
            return if (_value == null) null else _value::class
        }

    // type name as a string for single values
    private fun typeStrSingle(a: Any?): String = "${if (a != null) a::class.simpleName else "null"}"

    // type name as a string for array values
    private fun typeStr(a: Any?): String =
        if (isArrayValue)
            "[${if ((a as Iterable<*>).toList().isNotEmpty()) typeStrSingle(a.first()) else ""}]"
        else
            typeStrSingle(a)

    // pretty formatted JSON output
    companion object {
        val gson by lazy {
            JsonHelper.gsonExtended()
        }
    }

}

