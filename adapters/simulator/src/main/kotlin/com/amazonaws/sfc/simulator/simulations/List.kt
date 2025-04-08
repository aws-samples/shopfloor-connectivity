//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_ITEMS
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.createSimulationReader
import com.amazonaws.sfc.simulator.simulations.SimulationHelper.asAnyType
import com.google.gson.JsonObject
import kotlin.Any
import kotlin.collections.List
import kotlin.collections.map

class List (val list : List<Simulation>, val dataType: DataType) : Simulation {

    override fun value(): Any =
       list.map {
           val v = it.value()
           asAnyType(v, dataType)
       }

    companion object {


        val gson = createSimulationReader()

        fun fromJson(o: JsonObject): Simulation {

            val simulationName = this::class.java.simpleName
            if (o.get(CONFIG_DATA_TYPE) == null) {
                return InvalidSimulation(simulationName,"Simulation must have a data type in property $CONFIG_DATA_TYPE")
            }
            val dataType: DataType = DataType.fromString(o.get(CONFIG_DATA_TYPE).asString)

            val s = o.get(CONFIG_ITEMS) ?: return InvalidSimulation( simulationName,"$CONFIG_ITEMS must be set")
            if (!s.isJsonArray) return InvalidSimulation(simulationName,"CONFIG_ITEMS must be an array")
            val items = s.asJsonArray.map { item ->
                gson.fromJson(item, Simulation::class.java)
            }
            if (items.isEmpty()) return InvalidSimulation(simulationName,"$CONFIG_ITEMS is empty")

            return List(items, dataType)
        }

    }
}