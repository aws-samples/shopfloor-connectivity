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
import com.amazonaws.sfc.config.Validate
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["OutsideRangeInclusive"])
class OutsideRangeInclusive(operand: Range) : RangeTransformation(operand), Validate {


    @TransformerMethod
    override fun apply(target: Number?): Boolean? =

        if (operand == null || target == null) null
        else (target.toDouble() <= operand.minValue.toDouble() || target.toDouble() >= operand.maxValue.toDouble())

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a range value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)

        operand?.validate()
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = fromJson<OutsideRangeInclusive>(o)
        fun create(operand: Range) = OutsideRangeInclusive(operand)
    }


}