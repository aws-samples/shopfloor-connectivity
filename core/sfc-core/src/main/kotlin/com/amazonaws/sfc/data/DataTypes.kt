/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object DataTypes {

    // all unsigned number types
    private val unSignedNumbers = setOf(UByte::class, UShort::class, UInt::class, ULong::class)

    // test if a number is an unsigned number
    fun isUnsignedNumber(c: KClass<*>) = c in unSignedNumbers

    // tests if a value is numeric (signed or unsigned)
    fun isNumeric(k: KClass<*>?) = ((k != null) && ((k.isSubclassOf(Number::class)) || isUnsignedNumber(k)))

    @Suppress("unused")
    fun Class<*>.canBeAssignedFrom(from: Class<*>): Boolean {
        return assignableTypes(this, from)

    }

    fun assignableTypes(to: Class<*>?, from: Class<*>?): Boolean {

        if (to == null || from == null) return false

        if (to.isAssignableFrom(from)) return true

        if (isNumeric(to.kotlin) && isNumeric(from.kotlin)) return true

        // version 1.3.72 of Kotlin: unsigned types not subtypes of number, so test explicit
        return (isUnsignedNumber(from.kotlin) && from == Number::class.java)

    }


    fun asDoubleValue(value: Any?): Double? =
        when (value) {
            is UByte -> value.toDouble()
            is Byte -> (value).toDouble()
            is UShort -> (value).toDouble()
            is Short -> (value).toDouble()
            is UInt -> (value).toDouble()
            is Int -> (value).toDouble()
            is ULong -> (value).toDouble()
            is Long -> (value).toDouble()
            is Float -> (value).toDouble()
            is Double -> value
            else -> null
        }

    fun numericCompare(a: Any, b: Any): Int {
        val aa = asDoubleValue(a)
        val bb = asDoubleValue(b)
        if (aa == null || bb == null) {
            throw IllegalArgumentException("Can not compare value $a (${a::class.java.name}) with value $b (${b::class.java.name})")
        }
        return aa.compareTo(bb)
    }

    fun safeAsList(v: Iterable<*>): ArrayList<*> =
        if (v.count() == 1) arrayListOf(v.first()) else v as ArrayList<*>

}

