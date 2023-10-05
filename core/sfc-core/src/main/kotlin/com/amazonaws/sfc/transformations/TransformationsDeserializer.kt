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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.InvocationTargetException

import java.lang.reflect.Method
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation


class TransformationsDeserializer : JsonDeserializer<TransformationOperator> {

    data class OperatorCreateInstanceData(val name: String, val companionInstance: Any, val createMethod: Method)

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): TransformationOperator? {
        return json?.asJsonObject?.let { transformerFromJsonObject(it) }
    }

    private fun transformerFromJsonObject(o: JsonObject): TransformationOperator {

        val operatorPrimitive = o.getAsJsonPrimitive(CONFIG_TRANSFORMATION_OPERATOR)
        val operator =
            operatorPrimitive?.asString
            ?: return InvalidTransformationOperator(null, "Operator $CONFIG_TRANSFORMATION_OPERATOR must be specified", o.toString())

        val operatorData = knownOperators[operator]
                           ?: return InvalidTransformationOperator(operator, "Operator is unknown, it must be registered to ${this::class.simpleName} with method \"registerTransformation\" in order to deserialize it from its JSON configuration", o.toString())

        return try {
            operatorData.createMethod.invoke(operatorData.companionInstance, o) as TransformationOperator
        } catch (e: InvocationTargetException) {
            InvalidTransformationOperator(operator, e.targetException.toString(), o.toString())
        }
    }

    companion object {
        @Suppress("MemberVisibilityCanBePrivate") // keep public to allow registration o custom operators
        fun <T : TransformationOperator> registerTransformation(t: KClass<T>) {
            val companionInstance = t.companionObjectInstance ?: return

            val operatorName = t.simpleName.toString()
            val annotation = t.findAnnotation<TransformerOperator>()
            val names = annotation?.names ?: arrayOf(t.java.simpleName)
            names.forEach {
                val o = knownOperators[it]
                if (o != null) {
                    if (o.name != operatorName)
                        throw TransformationException("Registering operator $operatorName, operator name \"$it\" is already used for operator ${o.name}", operatorName)
                } else {
                    knownOperators[it] =
                        OperatorCreateInstanceData(t.simpleName.toString(), companionInstance, companionInstance::class.java.getDeclaredMethod("fromJson", JsonObject::class.java))
                }
            }
        }

        const val CONFIG_TRANSFORMATION_OPERATOR = "Operator"
        const val CONFIG_TRANSFORMATION_OPERAND = "Operand"

        private val knownOperators = mutableMapOf<String, OperatorCreateInstanceData>()

        init {
            listOf(
                Abs::class,
                Acos::class,
                And::class,
                Asin::class,
                Atan::class,
                AtIndex::class,
                BoolToNumber::class,
                BytesToInt16::class,
                Ceil::class,
                Celsius::class,
                Cos::class,
                Cosh::class,
                DecodeToString::class,
                Divide::class,
                Equals::class,
                Exp::class,
                Fahrenheit::class,
                Floor::class,
                Int16ToBytes::class,
                Int16sToInt32::class,
                Int32ToInt16s::class,
                IsoTimeStrToMilliSeconds::class,
                IsoTimeStrToNanoSeconds::class,
                IsoTimeStrToSeconds::class,
                Ln::class,
                Log2::class,
                Log10::class,
                LowerCase::class,
                MapRange::class,
                MapStringToNumber::class,
                Max::class,
                Min::class,
                Minus::class,
                Mod::class,
                Multiply::class,
                Not::class,
                Or::class,
                OutsideRangeExclusive::class,
                OutsideRangeInclusive::class,
                ParseInt::class,
                ParseNumber::class,
                Plus::class,
                Query::class,
                Round::class,
                Shl::class,
                Shr::class,
                Sin::class,
                Sign::class,
                Sinh::class,
                Sqrt::class,
                Str::class,
                StrEquals::class,
                StrEqualsNoCase::class,
                SubString::class,
                Tan::class,
                Tanh::class,
                ToByte::class,
                ToDouble::class,
                ToFloat::class,
                ToInt::class,
                ToLong::class,
                Trunc::class,
                TruncAt::class,
                UpperCase::class,
                WithinRangeExclusive::class,
                WithinRangeInclusive::class,
                Xor::class
            ).forEach { registerTransformation(it) }
        }

    }

}