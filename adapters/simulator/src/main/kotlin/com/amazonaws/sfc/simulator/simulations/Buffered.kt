//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations


import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ITEM
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.DEFAULT_INTERVAL
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.createSimulationReader
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getLong
import com.google.gson.JsonObject
import java.time.Instant

class Buffered (val simulation : Simulation, val interval : Long ) : Simulation {

    var nextValuesAt = System.currentTimeMillis() + interval

    val values = mutableListOf<Pair<Instant, Any?>>()

    override fun value(): Any? {
        val value = simulation.value()
        values.add(Instant.now() to value)
        return if (System.currentTimeMillis() >= nextValuesAt) {
            nextValuesAt = System.currentTimeMillis() + interval
           val data = values.map { Pair(it.first,it.second )}
            values.clear()
            data
        } else {
            null
        }
    }

    companion object {

        val gson = createSimulationReader()

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            val s = o.get(CONFIG_ITEM) ?: return InvalidSimulation(simulationName, "$CONFIG_ITEM must be set")
            val interval = o.getLong(CONFIG_INTERVAL, null)?:DEFAULT_INTERVAL
            val item = gson.fromJson(s, Simulation::class.java)

            if (item == null) return InvalidSimulation(simulationName,"$CONFIG_ITEM is empty")

            return Buffered(item, interval)
        }

    }
}