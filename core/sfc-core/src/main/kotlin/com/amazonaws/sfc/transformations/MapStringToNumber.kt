
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.util.*

@ConfigurationClass
@TransformerOperator(["MapStringToNumber"])
class MapStringToNumber(operand: Mapping?) : TransformationImpl<MapStringToNumber.Mapping>(operand) {

    // Do not rename Map and Default as these should match the case used for the JSON syntax
    class Mapping(val Map: HashMap<String, Int>, val Default: Int = 0) : Validate {
        override fun toString(): String {
            return "${Map.map { "\"${it.key}\":${it.value}" }}, Default=$Default)"
        }

        override fun validate() {
            if (validated) return

            ConfigurationException.check(
                (Map.isNotEmpty()),
                "Map can not be empty",
                "${this::class.simpleName}.Map",
                this)
            validated = true
        }

        private var _validated = false
        override var validated: Boolean
            get() = _validated
            set(value) {
                _validated = value
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Mapping) return false

            if (Map != other.Map) return false
            if (Default != other.Default) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Map.hashCode()
            result = 31 * result + Default
            return result
        }
    }


    @TransformerMethod
    fun apply(target: String?): Number? = operand?.Map?.getOrDefault(target.toString(), operand.Default)

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a Mapping instance",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this
        )
        operand?.validate()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapStringToNumber) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(operand)
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<MapStringToNumber, Mapping>(o) { op ->
            try {
                Gson().fromJson(op.toString(), Mapping::class.java)
            } catch (e: JsonSyntaxException) {
                throw TransformationException("Invalid operand \"$op\" for MapStringToNumber operator, $e", "MapStringToNumber")
            } catch (e: Exception) {
                throw TransformationException(e.toString(), "MapStringToNumber")
            }
        }

        fun create(operand: Mapping) = MapStringToNumber(operand)
    }


}