//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_STRUCT_PROPERTIES
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.createSimulationReader
import com.google.gson.JsonObject

class Structure(val struct : Map<String, Simulation>) : Simulation {
    override fun value(): Any =
        struct.mapValues { it.value.value() }

    companion object {

        val g = createSimulationReader()

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
                val s = o.get(CONFIG_STRUCT_PROPERTIES) ?: return InvalidSimulation(simulationName, "$CONFIG_STRUCT_PROPERTIES must be set")
                if (!s.isJsonObject) return InvalidSimulation(simulationName,"$CONFIG_STRUCT_PROPERTIES must be an object")
                val properties = s.asJsonObject.entrySet().associate { entry ->
                    val key = entry.key as String
                    val value = entry.value
                    if (!value.isJsonObject) return InvalidSimulation( simulationName,"$CONFIG_STRUCT_PROPERTIES must be an object")
                    key to g.fromJson(value, Simulation::class.java)
                }
            if (properties.isEmpty()) return InvalidSimulation(simulationName,"$CONFIG_STRUCT_PROPERTIES is empty")

            return Structure(properties)
        }

    }
}