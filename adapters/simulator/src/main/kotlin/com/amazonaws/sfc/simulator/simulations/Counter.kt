//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ARRAY_SIZE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DIRECTION
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MAX
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MIN
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_STEP
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getDouble
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getInt
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getString
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asNumericType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.maxValueForType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.minValueForType
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class Counter(val minValue: Double, val maxValue: Double, val step: Double, val direction: Direction, val dataType: DataType, val arraySize: Int) : Simulation {

    enum class Direction {
        @SerializedName("UP")
        UP,

        @SerializedName("DOWN")
        DOWN;

        companion object {
            fun fromString(value: String): Direction {
                return when (value) {
                    "UP" -> UP
                    "DOWN" -> DOWN
                    else -> throw IllegalArgumentException("Invalid Direction")
                }
            }
        }
    }

    private var counterValue: Double = if (direction == Direction.UP) minValue else maxValue

    fun next(): Double {
        // return counterValue in order to start at the indicate min of max value
        val currentValue = counterValue
        counterValue = when (direction) {
            Direction.UP -> {
                if (counterValue + step > maxValue) {
                    minValue
                } else {
                    (counterValue + step)
                }
            }

            Direction.DOWN -> {
                if (counterValue - step < minValue) {
                    maxValue
                } else {
                    (counterValue - step)
                }
            }
        }
        return currentValue
    }

    override fun value(): Any? {
        return if (arraySize == 0) asNumericType(next(), dataType)
        else {
            arrayListOf(*Array<Any?>(arraySize, { asNumericType(next(), dataType) }))
        }
    }

    companion object {

       fun fromJson(o: JsonObject): Simulation {

           val simulationName = this::class.java.simpleName
           if (o.get(CONFIG_DATA_TYPE) == null)  return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)
            val kotlinClass = dataType.kotlinClass

            val direction = try {
                Direction.Companion.fromString(o.getString(CONFIG_DIRECTION, "UP").toString().uppercase())
            } catch (_: Exception) {
                Direction.UP
            }
           val minValue = o.getDouble(CONFIG_MIN, null) ?: 0.0
           val min: Double = if (minValue == 0.0) 0.0 else maxOf(minValue, minValueForType(kotlinClass))
           val max = o.getDouble(CONFIG_MAX, null) ?: maxValueForType(kotlinClass)
           val step = o.getDouble(CONFIG_STEP, null) ?: 1.0
           val arraySize = o.getInt(CONFIG_ARRAY_SIZE, null) ?: 0

            return Counter(min, max, step, direction, dataType, arraySize)
        }
    }


}