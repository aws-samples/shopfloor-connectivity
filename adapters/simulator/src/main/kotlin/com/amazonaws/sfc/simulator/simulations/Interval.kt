//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ITEM
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.DEFAULT_INTERVAL
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.createSimulationReader
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.getLong
import com.google.gson.JsonObject

class Interval (val simulation : Simulation, val interval : Long ) : Simulation {

    var nextValueAt = System.currentTimeMillis() + interval

    override fun value(): Any? =
       if (System.currentTimeMillis() >= nextValueAt) {
           nextValueAt = System.currentTimeMillis() + interval
           simulation.value()
       } else {
           null
       }

    companion object {

        val gson = createSimulationReader()

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            val interval = o.getLong(CONFIG_INTERVAL, null)?:DEFAULT_INTERVAL

            val s = o.get(CONFIG_ITEM) ?: return InvalidSimulation(simulationName,"$CONFIG_ITEM must be set")
            val item = gson.fromJson(s, Simulation::class.java)

            if (item == null) return InvalidSimulation(simulationName,"$CONFIG_ITEM is empty")

            return Interval(item, interval)
        }

    }
}