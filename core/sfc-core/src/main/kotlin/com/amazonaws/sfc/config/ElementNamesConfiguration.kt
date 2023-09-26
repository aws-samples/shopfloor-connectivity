/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

/**
 * Class to configure the names of the names for timestamps, and values if combined with timestamps, in the output data
 */
@ConfigurationClass
class ElementNamesConfiguration {

    @SerializedName(CONFIG_SCHEDULE_NAME)
    private var _schedule = DEFAULT_SCHEDULE_VALUE

    /**
     * Name to use in output for schedule
     */
    val schedule: String
        get() = _schedule


    @SerializedName(CONFIG_SOURCES_NAME)
    private var _sources = DEFAULT_SOURCES_VALUE

    /**
     * Name to use in output for sources
     */
    val sources: String
        get() = _sources


    @SerializedName(CONFIG_VALUES_NAME)
    private var _values = DEFAULT_VALUES_NAME

    /**
     * Name to use in output for sources
     */
    val values: String
        get() = _values

    @SerializedName(CONFIG_VALUE_NAME)
    private var _value = DEFAULT_VALUE_NAME

    /**
     * Name to use in output for values if timestamp information is part of the output
     */
    val value: String
        get() = _value

    @SerializedName(CONFIG_TIMESTAMP_NAME)
    private var _timestamp = DEFAULT_TIMESTAMP_NAME

    /**
     * Name to use in output for timestamps
     */
    val timestamp: String
        get() = _timestamp

    @SerializedName(CONFIG_META_DATA_NAME)
    private var _metadata = DEFAULT_METADATA_NAME

    /**
     * Name to use in output for schedule
     */
    val metadata: String
        get() = _metadata

    @SerializedName(CONFIG_SERIAL_NAME)
    private var _serial = DEFAULT_SERIAL_VALUE

    /**
     * Name to use in output for schedule
     */
    val serial: String
        get() = _serial


    companion object {
        const val CONFIG_SCHEDULE_NAME = "Schedule"
        const val DEFAULT_SCHEDULE_VALUE = "schedule"
        const val CONFIG_SOURCES_NAME = "Sources"
        const val DEFAULT_SOURCES_VALUE = "sources"
        const val CONFIG_VALUES_NAME = "Values"
        const val DEFAULT_VALUES_NAME = "values"
        const val CONFIG_VALUE_NAME = "Value"
        const val DEFAULT_VALUE_NAME = "value"
        const val CONFIG_TIMESTAMP_NAME = "Timestamp"
        const val DEFAULT_TIMESTAMP_NAME = "timestamp"
        const val CONFIG_META_DATA_NAME = "Metadata"
        const val DEFAULT_METADATA_NAME = "metadata"
        const val CONFIG_SERIAL_NAME = "Serial"
        const val DEFAULT_SERIAL_VALUE = "serial"

        val DEFAULT_TAG_NAMES = ElementNamesConfiguration()

        fun create(schedule: String = DEFAULT_TAG_NAMES._schedule,
                   sources: String = DEFAULT_TAG_NAMES._sources,
                   values: String = DEFAULT_TAG_NAMES._values,
                   value: String = DEFAULT_TAG_NAMES._value,
                   timestamp: String = DEFAULT_TAG_NAMES._timestamp,
                   metadata: String = DEFAULT_TAG_NAMES._metadata,
                   serial: String = DEFAULT_TAG_NAMES._serial): ElementNamesConfiguration {

            val instance = ElementNamesConfiguration()

            @Suppress("DuplicatedCode")
            with(instance) {
                _schedule = schedule
                _sources = sources
                _values = values
                _value = value
                _timestamp = timestamp
                _metadata = metadata
                _serial = serial
            }
            return instance
        }


    }

}