// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.simulator

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.simulator.simulations.Simulation
import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class SimulatorAdapterConfigReader(configReader : ConfigReader) : ConfigReader(configReader.config, configReader.allowUnresolved, configReader.secretsManager) {
    override fun createJsonConfigReader(): Gson = GsonBuilder()
        .registerTypeAdapter(Simulation::class.java, SimulationDeserializer())
        .create()
}

