// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.data.DataTypes.isUnsignedNumber
import com.amazonaws.sfc.data.DataTypes.isUnsignedNumberOrListOfUnsigned
import com.amazonaws.sfc.log.Logger
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class TransformationImpl<T>(val operand: T? = null) : TransformationOperator {

    private val operatorName = this::class.simpleName ?: ""

    override operator fun invoke(target: Any, valueName: String, checkType: Boolean, throwsException: Boolean, logger: Logger?): Any? {

        val trace = logger?.getCtxTraceLog(this::class.java.simpleName, "invoke")

        val t = if (isUnsignedNumberOrListOfUnsigned(target)) target.toSigned() else target

        if (checkType && !validInput(t)) {
            if (throwsException) {
                val parameterTypeName = operatorMethod.parameters.first().type.simpleName
                val targetTypeName = target::class.simpleName
                throw TransformationException("invalid input type $targetTypeName for operator \"$operatorName\" with input parameter type $parameterTypeName", operatorName)
            }
            return null
        }
        // dealing with unsigned numbers, which are experimental in Kotlin version <= 1.4 and nu subclass of Number class
        val result = try {
            operatorMethod(this, t)
        } catch (e: Exception) {
            throw TransformationException("Error executing transformation operator $operatorName, $e", operatorName)
        }

        if (trace != null) {
            val valueNameStr = if (valueName != "") "$valueName :" else ""
            val noParams = (operand == null)
            val targetStr = "${if(target is String) "\"$target\"" else target}:${target::class.java.simpleName}"
            val paramStr = if (noParams) "$targetStr" else "$targetStr, $operand:${operand!!::class.java.simpleName}"
            val thread = Thread.currentThread().name
            var resultStr = (if (result is String) "\"$result\"" else "$result")
            resultStr = "$resultStr${if (result != null)":${result::class.java.simpleName}" else ""} on thread $thread"
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
    fun Any.toSigned(): Any = when (this) {
        is UByte -> this.toByte()
        is UShort -> this.toShort()
        is UInt -> this.toInt()
        is ULong -> this.toLong()
        is List<*> -> this.map { it?.toSigned() }
        else -> this
    }

    // Handling experimental unsigned data types signed -> unsigned

    fun Any.toUnsigned(): Any? =
        if (isUnsignedNumberOrListOfUnsigned(this)) this
        else

            try {
                when (this) {
                    is Byte -> this.toUByte()
                    is Short -> this.toUShort()
                    is Int -> this.toUInt()
                    is Long -> this.toULong()
                    is List<*> -> this.map { it?.toUnsigned() }
                    else -> this
                }
            } catch (_: Exception) {
                null
            }

    // test if type of input is valid for the operation performed by this instance
    private fun validInput(target: Any): Boolean {
        return validInputType(target)
    }

    /**
     * Test if input data type is valid for the operation performed by this instance
     * @param targetClass KClass<*>
     * @return Boolean
     */
    private fun validInputType(target: Any): Boolean {

        val targetClass = target::class.java.kotlin
        val isUnsigned = isUnsignedNumber(targetClass)
        return when {
            isUnsigned -> (inputType == Number::class.java || inputType == Any::class.java)
            target is ArrayList<*> -> inputType.isAssignableFrom(targetClass.java)
            target is Iterable<*> -> inputType.isAssignableFrom(targetClass.java)
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

    companion object {
        val List<Any>.listOfNumbers: List<Number>?
            get() {
                return this.map {
                    if (isNumeric(it::class)) it as Number
                    else {
                        if (it::class == String::class) try {
                            Integer.decode(it as String) as Number
                        } catch (_: NumberFormatException) {
                            return null
                        } else return null
                    }
                }
            }
    }


}