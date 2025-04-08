//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations


import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ARRAY_SIZE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_CYCLE_LENGTH
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MAX
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MIN
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_SHIFT
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.DEFAULT_CYCLE_LENGTH
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getDouble
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getInt
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getLong
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asNumericType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.maxValueForType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.minValueForType
import com.google.gson.JsonObject
import kotlin.math.sin

class Sinus(minValue: Double, maxValue: Double, val cycleLength: Long, shift : Int, val dataType: DataType, val arraySize: Int) : Simulation {

    private val delta = (maxValue - minValue) / 2
    private val x = (maxValue + minValue) / 2
    private val startTime = System.currentTimeMillis()
    private val timeShift = if (shift == 0) 0L else {
        val s : Double = (shift.toDouble() % 360) / 360
        (s * cycleLength).toLong()
    }

    val currentValue: Double
        get() {
            val pastSince = (System.currentTimeMillis() - startTime).toDouble() + timeShift
            val inCycle = (pastSince % cycleLength) / cycleLength
            val angle = (inCycle * 2 * Math.PI)
            val sin = sin(angle)

            val currentValue = x + sin * delta

            return currentValue

        }


    override fun value(): Any? {

        return if (arraySize == 0)
            asNumericType(currentValue, dataType)
        else arrayListOf(*Array<Any?>(arraySize, {
            asNumericType(currentValue, dataType)
        }))

    }

    companion object {

        fun fromJson(o: JsonObject): Simulation {
            val simulationName = this::class.java.simpleName
            if (o.get(CONFIG_DATA_TYPE) == null) return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)
            val kotlinClass = dataType.kotlinClass
            val minValue = o.getDouble(CONFIG_MIN, null) ?: 0.0
            val min: Double = if (minValue == 0.0) 0.0 else maxOf(minValue, minValueForType(kotlinClass))
            val max = minOf((o.getDouble(CONFIG_MAX, null) ?: 100.0), maxValueForType(kotlinClass))
            val cycleLength = o.getLong(CONFIG_CYCLE_LENGTH, null) ?: DEFAULT_CYCLE_LENGTH
            val arraySize = o.getInt(CONFIG_ARRAY_SIZE, null) ?: 0
            val shift = o.getInt(CONFIG_SHIFT, null) ?: 0

            return Sinus(min, max, cycleLength, shift, dataType, arraySize)

        }

    }


}
