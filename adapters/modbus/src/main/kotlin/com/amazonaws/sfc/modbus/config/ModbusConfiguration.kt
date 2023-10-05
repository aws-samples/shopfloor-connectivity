/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.config.SourceAdapterBaseConfiguration

/**
 * Modbus protocol sources configuration,
 */
@ConfigurationClass
abstract
class ModbusConfiguration : SourceAdapterBaseConfiguration() {

    /**
     * Configured Modbus sources
     */
    abstract val sources: Map<String, ModbusSourceConfiguration>

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateAtLeastOneSource()

        sources.values.forEach { source ->
            source.validate()
        }

        schedules.forEach { schedule ->
            validateSchedule(schedule)
        }
        validated = true
    }

    // checks that there is at least one source in the configuration
    private fun validateAtLeastOneSource() =
        ConfigurationException.check(
            (sources.isNotEmpty()),
            "Configuration must have 1 or more Modbus sources",
            CONFIG_SOURCES, this
        )


    // validates if sources in a schedule exist in the configuration
    private fun validateSchedule(schedule: ScheduleConfiguration) {
        schedule.sources.forEach { source ->
            val sourceConfig = this.sources[source.key]
            if (sourceConfig != null) {
                validateScheduleSources(source, sourceConfig, schedule)
            }
        }
    }

    // validates if the output channels for a schedule do exist in the configures sources
    private fun validateScheduleSources(sources: Map.Entry<String, ArrayList<String>>, source: ModbusSourceConfiguration, schedule: ScheduleConfiguration) {
        sources.value.forEach { channel ->
            ConfigurationException.check(
                (channel == WILD_CARD) || ((source.channels.containsKey(channel))),
                "Channel item \"$channel\" for source \"${sources.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                channel
            )
        }
    }


}