/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class StrRange : Validate {

    @SerializedName(CONFIG_RANGE_START)
    private var _start: Int = 0
    val start
        get() = _start

    @SerializedName(CONFIG_RANGE_END)
    private var _end: Int? = null
    val end
        get() = _end

    override fun validate() {
        if (validated) return
        validateStart()
        validated = true
    }


    private fun validateStart() {
        ConfigurationException.check(
            start >= 0,
            "$StrRange start value $CONFIG_RANGE_START must be >= 0",
            CONFIG_RANGE_START,
            this
        )
    }

    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    override fun toString(): String = "($CONFIG_RANGE_START=$start${if (end != null) ", $CONFIG_RANGE_END=$end)" else ")"}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StrRange) return false

        if (_start != other._start) return false
        if (_end != other._end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _start
        result = 31 * result + (_end ?: 0)
        return result
    }


    companion object {

        const val CONFIG_RANGE_START = "Start"
        const val CONFIG_RANGE_END = "End"

        private val default = StrRange()

        fun create(start: Int = default.start, end: Int? = default.end): StrRange {

            val instance = StrRange()
            with(instance) {
                _start = start
                _end = end
            }
            return instance
        }
    }

}