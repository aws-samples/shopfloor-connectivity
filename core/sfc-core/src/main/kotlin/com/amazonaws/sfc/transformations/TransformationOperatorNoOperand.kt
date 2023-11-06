
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.JsonObject

class TransformationOperatorNoOperand {

    companion object {
        inline fun <reified R : TransformationOperator> fromJson(o: JsonObject): TransformationOperator {
            val operand = o.get(TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERAND)
            if (operand != null) {
                return InvalidTransformationOperator(R::class.java.simpleName, "operator does not accept operands", o.toString())
            }
            return createInstance<R>(o)
        }

        inline fun <reified R : TransformationOperator> createInstance(o: JsonObject): TransformationOperator {
            val transformerConstructor =
                R::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
                ?: return InvalidTransformationOperator(operatorName = R::class.java.simpleName, message = "No parameterless constructor for Transformation Operator ${R::class.java.simpleName}", item = o.toString())
            return transformerConstructor.newInstance() as TransformationOperator
        }
    }
}