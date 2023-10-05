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

import com.amazonaws.sfc.data.DataTypes.assignableTypes
import com.amazonaws.sfc.log.Logger
import kotlin.reflect.KClass

typealias Transformation = List<TransformationOperator>


/**
 * Apply a transformation on a single value or an array or map of values.
 */
operator fun Transformation.invoke(target: Any?, valueName: String = "", throwsException: Boolean = true, logger: Logger? = null): Any? {

    if (target == null) {
        return null
    }

    return if (target is Iterable<*> && (!(assignableTypes(this.inputType, target::class.java)))) {
        // handle arrays of data
        target.toList().mapIndexed { i, v ->
            // apply operator on items
            if (v is Iterable<*>) {
                // item is an array, handle nested arrays by recursion
                v.filterNotNull().toList().map { vv ->
                    this.invoke(vv, throwsException = throwsException, valueName = valueName, logger = logger)
                }
            } else {
                // element is single value
                applyOnSingleValue(v, checkType = i == 0, valueName = valueName, logger = logger, throwsException = throwsException)
            }
        }
    } else
    // single value
        applyOnSingleValue(target, checkType = true, valueName = valueName, logger = logger, throwsException = throwsException)
}

/**
 * Apply a transformation on a single value
 */
fun Transformation.applyOnSingleValue(target: Any?,
                                      valueName: String = "",
                                      checkType: Boolean = false,
                                      throwsException: Boolean = true,
                                      logger: Logger? = null): Any? {

    var result = target
    for (t in this) {
        if (result == null) break
        result = t.invoke(target = result, valueName = valueName, checkType = checkType, throwsException = throwsException, logger = logger)
    }
    return result
}

val Transformation.inputType
    get() = if (this.isNotEmpty()) this.firstOrNull()?.inputType else null

fun Transformation.isValidInput(value: Any): Boolean {
    return isValidInputType(value::class)
}

val Transformation.resultType
    get() = if (this.isNotEmpty()) this.lastOrNull()?.resultType else null

fun Transformation.isValidInputType(c: KClass<*>): Boolean {
    return isValidInputType(c.java)
}


private fun Transformation.isValidInputType(c: Class<*>): Boolean {
    return ((inputType != null) &&
            assignableTypes(inputType!!, c))
}


fun Transformation.validateOperatorTypes(): TransformValidationError? {

    if (this.isEmpty()) {
        return null
    }

    if (this.size == 1)
        if (this.first() is InvalidTransformationOperator) {
            val e = this.first() as InvalidTransformationOperator
            return TransformValidationError(Operator = e,
                Order = 0,
                Error = "Transformation operator ${e.operatorName} is an invalid operator, $e}")
        }

    var operator = this.first()
    var operatorOutputType = operator.resultType


    var order = 0

    while (order < this.size - 1) {

        val nextOperator = this[order + 1]

        if (nextOperator is InvalidTransformationOperator) {

            return TransformValidationError(Operator = nextOperator,
                Order = order + 2,
                Error = "Transformation operator ${nextOperator.operatorName} is an invalid operator, $nextOperator}")
        }

        if (operatorOutputType != Any::class.java) {

            val nextInputType = nextOperator.inputType

            if (nextInputType != Any::class.java) {

                if (!assignableTypes(nextOperator.inputType, operatorOutputType)) {
                    return TransformValidationError(
                        Operator = nextOperator,
                        Order = order + 1,
                        Error = "operator output type '${operatorOutputType.simpleName}' of operator ${operator::class.simpleName} is invalid for input type '${nextInputType.simpleName}' of the next operator ${nextOperator::class.simpleName}"
                    )
                }
            }
        }

        operator = nextOperator
        operatorOutputType = operator.resultType
        order += 1
    }
    return null
}




