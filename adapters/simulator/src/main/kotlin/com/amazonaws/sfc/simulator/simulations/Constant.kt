//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations


import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_VALUE

import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.asDouble
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.jsonToNative
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asNumericType
import com.google.gson.JsonObject
import kotlin.collections.List
import kotlin.collections.map

open class Constant(val value: Any?, val dataType: DataType) : Simulation {

    override fun value(): Any? {
        return if (value is List<*>) {
            value.map { if (it != null) asType(it, dataType) else null }
        } else {
            asType(value, dataType)
        }
    }

    companion object {

        fun asType(value: Any?, type: DataType): Any? {
            return if (value!=null) when (type) {
                DataType.BOOLEAN -> when (value) {
                    is Boolean -> value
                    is String -> {
                        when (value.toString().lowercase()) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                    }

                    is Double -> value == 1.0
                    else -> null

                }
                DataType.STRING -> value.toString()
                DataType.CHAR -> value.toString().firstOrNull()
                else ->  asNumericType(asDouble(value), type)
            }
            else null
        }

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            val constantValue = o.get(CONFIG_VALUE) ?: return InvalidSimulation(simulationName, "$CONFIG_VALUE must be set")
            if (o.get(CONFIG_DATA_TYPE) == null) return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")

            if (o.get(CONFIG_DATA_TYPE) == null) return InvalidSimulation(simulationName,"CSimulation must have a data type in property $CONFIG_DATA_TYPE")
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)

            val value = asType(jsonToNative(constantValue), dataType)

            return Constant(value, dataType)

        }

    }
}