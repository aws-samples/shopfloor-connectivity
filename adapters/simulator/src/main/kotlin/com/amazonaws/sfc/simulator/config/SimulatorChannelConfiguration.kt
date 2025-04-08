// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.simulator.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_SIMULATION
import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.CONFIG_SIMULATION_TYPE
import com.amazonaws.sfc.simulator.simulations.InvalidSimulation
import com.amazonaws.sfc.simulator.simulations.Simulation
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class SimulatorChannelConfiguration : ChannelConfiguration() {


    @SerializedName(CONFIG_SIMULATION)
    private var _simulation : Simulation? = null
    val simulation : Simulation?
        get() = _simulation

    override fun validate() {

        ConfigurationException.check(
            _simulation!=null,
            "$CONFIG_SIMULATION_TYPE is not provided for channel",
            CONFIG_SIMULATION_TYPE,
            this)

        if (_simulation is InvalidSimulation) throw ConfigurationException(
            "$CONFIG_SIMULATION_TYPE \"${(_simulation as InvalidSimulation).simulationName}\" is not valid, ${(_simulation as InvalidSimulation).reason}",
            CONFIG_SIMULATION_TYPE,
            this
        )
        validated = true

    }


    companion object {

        private val default = SimulatorChannelConfiguration()

        fun create(name: String? = default._name,
                   description: String = default._description,
                   simulation: Simulation? = default._simulation,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID,
                   conditionFilter: String? = default._conditionFilterID): SimulatorChannelConfiguration {

            val instance = createChannelConfiguration<SimulatorChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter,
                conditionFilter = conditionFilter)

            with(instance) {
                _simulation = simulation
            }
            return instance
        }

    }


}








