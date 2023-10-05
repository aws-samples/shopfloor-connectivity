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

import com.google.gson.JsonElement
import com.google.gson.JsonObject

abstract class TransformationOperatorWithOperand {

    companion object {

        inline fun <reified R : TransformationOperator, reified P> fromJson(o: JsonObject, fn: (jsonObject: JsonElement) -> Any): TransformationOperator {
            val operand = o.get(TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERAND)
                          ?: throw TransformationException(R::class.java.simpleName, "Transformation  ${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERAND} can not be null")
            return createInstance<R, P>(operand, fn)
        }

        inline fun <reified R : TransformationOperator, reified P> createInstance(operand: JsonElement,
                                                                                  fn: (jsonObject: JsonElement) -> Any): TransformationOperator {
            val transformerConstructor =
                R::class.java.constructors.firstOrNull { it.parameters.size == 1 && it.parameterTypes[0].isAssignableFrom(P::class.java) }
            assert(transformerConstructor != null)
            val p: P = fn(operand) as P
            return transformerConstructor!!.newInstance(p) as TransformationOperator
        }
    }
}
