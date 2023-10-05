/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["And", "&"])
class And(operand: Number) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (operand == null) null
        else when (target) {
            is Int -> target and operand.toInt()
            is Byte -> (target.toInt() and operand.toInt()).toByte()
            is Short -> (target.toInt() and operand.toInt()).toShort()
            is Long -> target.toLong() and operand.toLong()
            is Double -> (target.toLong() and operand.toLong()).toDouble()
            is Float -> (target.toLong() and operand.toLong()).toFloat()
            else -> null
        }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<And>(o)
        fun create(operand: Number) = And(operand)
    }

}