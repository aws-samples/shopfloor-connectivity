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

import com.amazonaws.sfc.data.DataTypes.isUnsignedNumber
import com.amazonaws.sfc.log.Logger
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class TransformationImpl<T>(val operand: T? = null) : TransformationOperator {

    private val operatorName = this::class.simpleName ?: ""

    override operator fun invoke(target: Any, valueName: String, checkType: Boolean, throwsException: Boolean, logger: Logger?): Any? {

        val trace = logger?.getCtxTraceLog(this::class.java.simpleName, "invoke")

        if (checkType && !validInput(target)) {
            if (throwsException) {
                val parameterTypeName = operatorMethod.parameters.first().type.simpleName
                val targetTypeName = target::class.simpleName
                throw TransformationException("invalid input type $targetTypeName for operator \"$operatorName\" with input parameter type $parameterTypeName", operatorName)
            }
            return null
        }
        // dealing with unsigned numbers, which are experimental in Kotlin version <= 1.4 and nu subclass of Number class
        val result = try {
            if (isUnsignedNumber(target::class))
                (operatorMethod(this, target.toSigned()) as Long).toUnsigned(target)
            else
                operatorMethod(this, target)
        } catch (e: Exception) {
            throw TransformationException("Error executing transformation operator $operatorName, $e", operatorName)
        }

        if (trace != null) {
            val valueNameStr = if (valueName != "") "$valueName :" else ""
            val noParams = (operand == null)
            val targetStr = if (target is String) "\"$target\"" else target
            val paramStr = if (noParams) "$targetStr" else "$targetStr, $operand"
            val thread = Thread.currentThread().name
            var resultStr = if (result is String) "\"$result\"" else "$result"
            resultStr = "$resultStr (${if (result != null) result::class.java.simpleName else ""}) on thread $thread"
            trace.invoke("$valueNameStr$operatorName($paramStr) => $resultStr")
        }
        return result

    }

    private val operatorMethod: Method by lazy {
        this::class.java.declaredMethods.firstOrNull { m -> m.annotations.firstOrNull { it is TransformerMethod } != null }
        ?: throw TransformationException("Transformation does not have a apply method", operatorName)
    }

    override val inputType: Class<*> by lazy {
        operatorMethod.parameters.first().type
    }

    override val resultType: Class<*> by lazy {
        operatorMethod.returnType
    }


    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    fun isNumeric(k: KClass<*>?) = ((k != null) && ((k.isSubclassOf(Number::class)) || isUnsignedNumber(k)))

    // Handling experimental unsigned data types unsigned -> signed
    private fun Any.toSigned(): Any = when (this) {
        is UByte -> this.toLong()
        is UShort -> this.toLong()
        is UInt -> this.toLong()
        else -> (this as ULong).toLong()
    }

    // Handling experimental unsigned data types signed -> unsigned
    private fun Long.toUnsigned(org: Any): Any = when (org) {
        is UByte -> this.toUByte()
        is UShort -> this.toUShort()
        is UInt -> this.toUInt()
        else -> this.toULong()
    }

    // test if type of input is valid for the operation performed by this instance
    private fun validInput(target: Any): Boolean {
        return validInputType(target::class)
    }

    /**
     * Test if input data type is valid for the operation performed by this instance
     * @param targetClass KClass<*>
     * @return Boolean
     */
    private fun validInputType(targetClass: KClass<*>): Boolean {

        val isUnsigned = isUnsignedNumber(targetClass)
        return when {
            isUnsigned -> (inputType == Number::class.java || inputType == Any::class.java)
            targetClass is Iterable<*> -> inputType.kotlin is Iterable<*>
            else -> (targetClass.isSubclassOf(inputType.kotlin))
        }

    }


    override fun toString(): String {
        return "${this::class.simpleName}${if (operand != null) "($operand)" else "()"}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformationImpl<*>) return false

        if (operand != other.operand) return false
        if (operatorName != other.operatorName) return false

        return true
    }


}