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
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Fahrenheit"])
class Fahrenheit : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (target == null || !isNumeric(target::class)) null else
            when (target) {
                is Float -> ((target.toFloat() * 1.8f) + 32f)
                else -> ((target.toDouble() * 1.8) + 32.0)
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Fahrenheit>(o)
        fun create() = Fahrenheit()
    }
}