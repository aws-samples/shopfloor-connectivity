//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.simulation

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ARRAY_SIZE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MAX
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_MIN
import com.amazonaws.sfc.simulator.simulations.DataType
import com.amazonaws.sfc.simulator.simulations.InvalidSimulation
import com.amazonaws.sfc.simulator.simulations.Simulation
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getDouble
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getInt
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asByte
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asFloat
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asInt
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asShort
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asUByte
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asUInt
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asULong
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asUShort
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.maxValueForType
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.minValueForType
import com.google.gson.JsonObject
import kotlin.random.Random

class Random(val minValue: Double, val maxValue: Double, val dataType: DataType, val arraySize: Int = 0) : Simulation {

    private val rand = Random(System.currentTimeMillis())

    fun next(): Any? = when(dataType){
            DataType.BOOLEAN -> rand.nextBoolean()
            DataType.BYTE -> asByte((asByte(minValue) as Byte..asByte(maxValue) as Byte).random())
            DataType.DOUBLE -> rand.nextDouble(minValue, maxValue+1)
            DataType.FLOAT -> asFloat(rand.nextDouble(minValue, maxValue))
            DataType.INT -> rand.nextInt(minValue.toInt(), maxValue.toInt())
            DataType.LONG -> rand.nextLong(minValue.toLong(), maxValue.toLong())
            DataType.SHORT ->  asShort((asShort(minValue) as Short..asShort(maxValue) as Short).random())
            DataType.UBYTE ->  asUByte(rand.nextInt(asInt(minValue) as Int, asInt(maxValue) as Int))
            DataType.UINT -> asUInt(rand.nextLong(minValue.toLong(), maxValue.toLong()))
            DataType.ULONG ->  asULong(rand.nextDouble(minValue, maxValue))
            DataType.USHORT -> asUShort(rand.nextInt(asInt(minValue) as Int, asInt(maxValue) as Int))
            else -> null}

    override fun value(): Any? {

        return when (arraySize) {
            0 -> next()
            else -> arrayListOf(*Array<Any?>(arraySize, { (next()) }))

        }
    }

    companion object {

        fun  fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            if (o.get(CONFIG_DATA_TYPE) == null) {
                return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
            }
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)
            val kotlinClass = dataType.kotlinClass

            val min: Double = o.getDouble(CONFIG_MIN, null) ?: (minValueForType(kotlinClass))
            val max = o.getDouble(CONFIG_MAX, null) ?: (maxValueForType(kotlinClass) - 1.0)
            val size = o.getInt(CONFIG_ARRAY_SIZE, null) ?: 0

            return Random(min, max, dataType, size)
        }


    }
}