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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlin.math.round


@ConfigurationClass
@TransformerOperator(["MapRange"])
class MapRange(operand: Map? = null) : TransformationImpl<MapRange.Map>(operand) {

    class Map : Validate {

        @SerializedName(CONFIG_MAP_FROM)
        private var _from: Range? = null
        var from
            get() = _from!!
            set(value) {
                _from = value
            }

        @SerializedName(CONFIG_MAP_TO)
        private var _to: Range? = null
        var to
            get() = _to!!
            set(value) {
                _to = value
            }

        val slope: Double
            get() =
                ((to.maxValue.toDouble() - to.minValue.toDouble()) / (from.maxValue.toDouble() - from.minValue.toDouble()))

        private var _validated = false
        override var validated: Boolean
            get() = _validated
            set(value) {
                _validated = value
            }

        override fun validate() {
            if (validated) return

            ConfigurationException.check(
                (_from != null),
                "$CONFIG_MAP_FROM must be specified for range mapping",
                CONFIG_MAP_FROM,
                this
            )

            ConfigurationException.check(
                (_to != null),
                "$CONFIG_MAP_TO must be specified for range mapping",
                CONFIG_MAP_TO,
                this
            )

            from.validate()
            to.validate()

            validated = true
        }


        override fun toString(): String {
            return "($CONFIG_MAP_FROM=$from, $CONFIG_MAP_TO=$to)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Map) return false

            if (_from != other._from) return false
            if (_to != other._to) return false

            return true
        }


        companion object {
            fun create(to: Range, from: Range): Map {
                val instance = Map()
                instance.from = from
                instance.to = to
                return instance
            }


        }
    }


    @TransformerMethod
    fun apply(target: Number?): Number? {

        if ((operand == null || target == null) ||
            ((target.toDouble() < operand.from.minValue.toDouble()) || (target.toDouble() > operand.from.maxValue.toDouble())) ||
            ((operand.from.minValue == operand.from.maxValue) || (operand.to.minValue == operand.to.maxValue)))
            return null


        val mappedValue = operand.to.minValue.toDouble() + operand.slope * (target.toDouble() - operand.from.minValue.toDouble())

        return when (target) {
            is Int -> round(mappedValue).toLong()
            is Byte -> round(mappedValue).toLong()
            is Long -> round(mappedValue).toLong()
            is Float -> mappedValue.toFloat()
            else -> mappedValue
        }

    }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a MapRange.Mapping instance",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this
        )

        operand?.validate()
    }


    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<MapRange, Map>(o) { op ->
            try {
                Gson().fromJson(op.toString(), Map::class.java)
            } catch (e: Exception) {
                throw TransformationException("Error reading operator \"$op\" for MapRange operand, $e", "MapRange")
            }
        }

        fun create(operand: Map) = MapRange(operand)

        const val CONFIG_MAP_FROM = "From"
        const val CONFIG_MAP_TO = "To"
    }

}