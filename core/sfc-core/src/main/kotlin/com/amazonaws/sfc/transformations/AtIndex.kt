/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
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
import com.amazonaws.sfc.transformations.TransformationsDeserializer.Companion.CONFIG_TRANSFORMATION_OPERATOR
import com.google.gson.JsonObject
import kotlin.math.absoluteValue

@ConfigurationClass
@TransformerOperator(["AtIndex", "[]"])
class AtIndex(operand: Number) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: List<Any>): Any? {

        val position: Int? = operand?.toInt()
        if ((position == null) || (position < 0 && position.absoluteValue > target.size) || (position >= target.size)) return null
        val index = if (position >= 0) position else target.size + position
        return target.getOrNull(index)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<AtIndex>(o)
        fun create(operand: Number) = AtIndex(operand)
    }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (operand != null),
            "Operand for  ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.$CONFIG_TRANSFORMATION_OPERATOR",
            this
        )
        validated = true
    }

}