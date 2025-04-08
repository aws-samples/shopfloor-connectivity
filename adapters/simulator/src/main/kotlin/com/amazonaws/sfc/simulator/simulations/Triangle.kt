//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ARRAY_SIZE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_CYCLE_LENGTH
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MAX
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MIN
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.DEFAULT_CYCLE_LENGTH
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getDouble
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getInt
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getLong
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asNumericType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.maxValueForType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.minValueForType
import com.google.gson.JsonObject

class Triangle(val minValue: Double, val maxValue: Double, cycleLength: Long?, val dataType: DataType, val arraySize: Int) : Simulation {

    private val range = (maxValue - minValue) + 1
    private val startTime = System.currentTimeMillis()
    val length = cycleLength ?: DEFAULT_CYCLE_LENGTH


    override fun value(): Any? {

        return if (arraySize == 0)
            asNumericType(currentValue, dataType)
        else arrayListOf(*Array<Any?>(arraySize, {
            asNumericType(currentValue, dataType)
        }))
    }

    val currentValue: Double
        get() {
            val pastSince = (System.currentTimeMillis() - startTime).toDouble()
            val inCycle = (pastSince % length) / length
            val currentValue =when{
                inCycle < 0.25 -> minValue + (range * 4 * inCycle)
                inCycle >= 0.25 && inCycle < 0.5 -> maxValue - (range * 4 * (inCycle - 0.25))
                inCycle >= 0.5 && inCycle < 0.75 -> minValue + (range * 4 * (inCycle - 0.5))
                else -> maxValue - (range * 4 * (inCycle - 0.75))
            }
            return minOf(maxOf(currentValue, minValue), maxValue)
        }

    companion object {

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            if (o.get(CONFIG_DATA_TYPE) == null) return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
                val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)
                val kotlinClass = dataType.kotlinClass

                val size =  o.getInt(CONFIG_ARRAY_SIZE, null) ?: 0
                val min = maxOf((o.getDouble(CONFIG_MIN, null) ?: 0.0), minValueForType(kotlinClass))
                val max = minOf((o.getDouble(CONFIG_MAX, null) ?: 100.0), maxValueForType(kotlinClass))
                val length = o.getLong(CONFIG_CYCLE_LENGTH, null) ?: DEFAULT_CYCLE_LENGTH

                return Triangle(min, max, length, dataType, size)

        }

    }
}

