/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.google.gson.JsonObject
import kotlin.math.pow
import kotlin.math.truncate


@ConfigurationClass
@TransformerOperator(["TruncAt"])
class TruncAt(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        if (operand == null) return null

        val p = operand.toInt()
        if ((target is Float) || (target is Double)) {
            val m = (10.0).pow(p)
            when (target) {
                is Float -> return (truncate(target * m).toFloat() / m).toFloat()
                is Double -> return (truncate(target * m) / m)
            }
        } else {
            if (p < 0) {
                if (10.0.pow(p * -1) > target!!.toDouble()) {
                    return 0
                }

                val m = 10.0.pow(p)
                val result = (truncate(target.toDouble() * m)) / m
                when (target) {
                    is Int -> return result.toInt()
                    is Byte -> return result.toInt().toByte()
                    is Short -> return result.toInt().toShort()
                    is Long -> return result.toLong()
                }
            }
        }
        return target
    }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<TruncAt>(o)
        fun create(operand: Int) = TruncAt(operand)
    }

}