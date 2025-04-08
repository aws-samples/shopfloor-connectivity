//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ARRAY_SIZE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_RANDOM
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_VALUES
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getInt
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.jsonToNative
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asAnyType
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.google.gson.JsonObject
import java.util.*
import kotlin.collections.List
import kotlin.collections.map

class Range(val values: List<Any?>, val random: Boolean = false, val dataType: DataType, val arraySize : Int) : Simulation {

    val rnd: Random? = if (random) Random(systemDateTime().toEpochMilli()) else null

    var index: Int = -1

    override fun value(): Any? =
        if (arraySize == 0) currentValue() else (0 until arraySize).map { currentValue() }

    private fun currentValue(): Any? = if (random) {
        val i = rnd!!.nextInt(values.size)
        values[i]
    } else {
        index += 1
        if (index >= values.size) index = 0
        values[index]
    }


    companion object {

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            if (o.get(CONFIG_DATA_TYPE) == null) {
                return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
            }
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)
            dataType.kotlinClass

            val values = o.get(CONFIG_VALUES) ?: return InvalidSimulation(simulationName,"$CONFIG_VALUES must be set")
            if (!values.isJsonArray) return InvalidSimulation( simulationName,"$CONFIG_VALUES must be an array of values")
            if (values.asJsonArray.size() == 0) return InvalidSimulation(simulationName,"$CONFIG_VALUES must have at least one value")

            val random = o.get(CONFIG_RANDOM) != null && o.get(CONFIG_RANDOM).asBoolean

            val arraySize =  o.getInt(CONFIG_ARRAY_SIZE, null) ?: 0

            val rangeValues = values.asJsonArray.map { item ->
                deepCast(jsonToNative(item), dataType)
            }
            return Range(rangeValues, random, dataType, arraySize)

        }

        fun deepCast(value: Any?, dataType: DataType): Any? {
            return when (value) {
                null -> null
                is List<*> -> sequence<Any?> {
                    for (v in value)
                        if (v != null) yield(deepCast(v, dataType))
                }.toList()

                else -> asAnyType(value, dataType)
            }
        }

    }
}