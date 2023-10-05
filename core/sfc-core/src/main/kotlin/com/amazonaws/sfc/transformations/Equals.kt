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
@TransformerOperator(["Equals", "=="])
class Equals(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Boolean =

        if (operand == null) (target == null)
        else when (target) {
            is Int -> target == operand.toInt()
            is Float -> target == operand.toFloat()
            is Double -> target == operand.toDouble()
            is Byte -> target == operand.toByte()
            is Short -> target == operand.toShort()
            is Long -> target == operand.toLong()
            else -> false
        }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Equals>(o)
        fun create(operand: Number?) = Equals(operand)
    }

}