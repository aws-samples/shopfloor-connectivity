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

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.transformations.TransformationsDeserializer.Companion.CONFIG_TRANSFORMATION_OPERAND
import com.google.gson.Gson
import com.google.gson.JsonObject


abstract class RangeTransformation(operand: Range) : TransformationImpl<Range>(operand), Validate {

    abstract fun apply(target: Number?): Boolean?

    companion object {
        inline fun <reified T : RangeTransformation> fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<T, Range>(o) { op ->
            try {
                Gson().fromJson(op.toString(), Range::class.java)
            } catch (e: Exception) {
                throw ConfigurationException("Invalid Json \"${op}\" for Range operand, $e", CONFIG_TRANSFORMATION_OPERAND)
            }
        }
    }


    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (operand != null),
            "Range operand must be set",
            CONFIG_TRANSFORMATION_OPERAND,
            this
        )

        operand!!.validate()

        validated = true
    }


}