
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
