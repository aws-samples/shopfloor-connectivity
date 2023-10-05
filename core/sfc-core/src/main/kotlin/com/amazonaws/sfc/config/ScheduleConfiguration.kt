/*
 *
 *     Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *     Licensed under the Amazon Software License (the "License"). You may not use this file except in
 *     compliance with the License. A copy of the License is located at :
 *
 *     http://aws.amazon.com/asl/
 *
 *     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *
 */

package com.amazonaws.sfc.config

import com.amazonaws.sfc.aggregations.AggregationConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ACTIVE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DISABLED_COMMENT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_META_DATA
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_NAME
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Configuration for an SFC schedule.
 */
@ConfigurationClass
class ScheduleConfiguration : Validate {
    @SerializedName(CONFIG_NAME)
    private var _name = ""

    /**
     * Name of the schedule
     */
    val name: String
        get() = _name

    @SerializedName(CONFIG_DESCRIPTION)
    private var _description = ""

    /**
     * Description of the schedule
     */
    val description: String
        get() = _description

    @SerializedName(CONFIG_ACTIVE)
    private var _active = true

    /**
     * Active state of the schedule, set false to exclude schedule from running. Note that a schedule
     * must have at least 1 active schedule.
     */
    val active: Boolean
        get() = _active

    @SerializedName(CONFIG_SCHEDULE_INTERVAL)
    private var _interval: Long = DEFAULT_INTERVAL_MS

    /**
     * Interval in which values are read from the source
     */
    val interval: Duration
        get() = _interval.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_SCHEDULE_SOURCES)
    private var _sources = mapOf<String, ArrayList<String>>()

    /**
     * Input sources and channels for a schedule
     */
    val sources: Map<String, ArrayList<String>>
        get() = _sources.filter { !it.key.startsWith(CONFIG_DISABLED_COMMENT) }

    @SerializedName(CONFIG_TARGETS)
    private var _targets = ArrayList<String>()

    /**
     * Output targets for a schedule
     */
    val targets: ArrayList<String>
        get() = _targets

    @SerializedName(CONFIG_TIMESTAMP_LEVEL)
    private var _timestampLevel: TimestampLevel = TimestampLevel.NONE

    @SerializedName(CONFIG_META_DATA)
    private var _metadata = emptyMap<String, String>()

    /**
     * Metadata, which are constant values, that will be added as constant values for a source
     */
    val metadata: Map<String, String>
        get() = _metadata


    /**
     * Level of timestamps to include in output.
     * @see TimestampLevel
     */
    val timestampLevel: TimestampLevel
        get() = _timestampLevel

    @SerializedName(CONFIG_AGGREGATION)
    private var _aggregation: AggregationConfiguration? = null

    /**
     * Aggregation configuration for a schedule
     * @see AggregationConfiguration
     */
    val aggregation: AggregationConfiguration?
        get() = _aggregation

    /**
     * Size of aggregation, 0 if no aggregation is applied
     */
    val aggregationSize: Int
        get() = aggregation?.size ?: 0

    /**
     * Returns true if aggregation is applied
     */
    val isAggregated: Boolean
        get() = aggregationSize > 0

    /**
     * Returns the IDs of all sources that are used in active schedules in the configuration
     */
    val activeSourceIDs: Set<String>
        get() {
            return if (active)
                sources.filter { i -> i.value.isNotEmpty() }
                    .map { it.key }.toHashSet()
            else
                emptySet()
        }


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        validateScheduleName()
        validateInterval()
        validateInputs()
        validateInputChannels()
        validateTargets()
        aggregation?.validate()
        validated = true
    }

    // Validates if a schedule has targets
    private fun validateTargets() =
        ConfigurationException.check(
            targets.isNotEmpty(),
            "Schedule \"$name\" must have 1 or more targets",
            CONFIG_TARGETS,
            this
        )

    // Validates if a schedule has inputs
    private fun validateInputChannels() =
        sources.forEach { (k, v) ->
            ConfigurationException.check(
                (v.isNotEmpty()),
                "$CONFIG_SCHEDULE_SOURCES \"$k\" for schedule \"$name\" must have 1 or more values",
                CONFIG_SCHEDULE_SOURCES,
                this.sources
            )
        }

    // Validates if a schedule has inputs
    private fun validateInputs() =
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Schedule \"$name\"must have 1 or more inputs",
            CONFIG_SCHEDULE_SOURCES,
            this
        )

    // Validate schedule interval
    private fun validateInterval() =
        ConfigurationException.check(
            (interval > 0.toDuration(DurationUnit.MILLISECONDS)),
            "$CONFIG_SCHEDULE_INTERVAL must be 1 (milliseconds) or longer",
            CONFIG_SCHEDULE_INTERVAL,
            this
        )

    // Validates schedule name
    private fun validateScheduleName() =
        ConfigurationException.check(
            name.isNotBlank(),
            "Schedule name can not be empty",
            CONFIG_NAME,
            this
        )


    companion object {
        const val DEFAULT_INTERVAL_MS = 1000L
        const val CONFIG_SCHEDULE_INTERVAL = "Interval"
        const val CONFIG_SCHEDULE_SOURCES = "Sources"
        const val CONFIG_TIMESTAMP_LEVEL = "TimestampLevel"
        const val CONFIG_AGGREGATION = "Aggregation"

        private val default = ScheduleConfiguration()

        fun create(name: String = default._name,
                   description: String = default._description,
                   active: Boolean = default._active,
                   interval: Long = default._interval,
                   sources: Map<String, ArrayList<String>> = default._sources,
                   targets: ArrayList<String> = default._targets,
                   timestampLevel: TimestampLevel = default._timestampLevel,
                   metadata: Map<String, String> = default._metadata,
                   aggregation: AggregationConfiguration? = default._aggregation): ScheduleConfiguration {

            val instance = ScheduleConfiguration()

            with(instance) {
                _name = name
                _description = description
                _active = active
                _interval = interval
                _sources = sources
                _targets = targets
                _timestampLevel = timestampLevel
                _metadata = metadata
                _aggregation = aggregation
            }
            return instance
        }

    }

}

