/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ControllerServiceConfiguration
import com.amazonaws.sfc.log.Logger

/**
 * Class that contains change filters for all channels
 * @property configuration ControllerServiceConfiguration controller configuration
 * @constructor
 */
class ChangeFiltersCache(private val configuration: ControllerServiceConfiguration) {

    private val className = this::class.java.simpleName

    // map with an entry for every source, each holding a map with an entry for the channel in that source containing the change filter
    private val filters = mutableMapOf<String, MutableMap<String, Filter>>()

    /**
     * Get the filter for the specified source/channel. A filter is created if it does not exist
     * @param source String Source ID
     * @param channel String ChannelID
     * @return Filter? Change filter to apply
     */
    private fun getChangeFilter(source: String, channel: String): Filter? {

        // Test if filter already exists
        var filter: Filter? = filters[source]?.get(channel)
        if (filter != null) return filter

        // Get config for both source and channel
        val sourceConfig = configuration.sources[source] ?: return null
        val channelConfig = sourceConfig.channels[channel] ?: return null

        // Create filter for channel
        // If a filter at source level exists it is used for all channel, unless it is overwritten with a filter at channel level
        val filterConfigID: String? = channelConfig.changeFilterID ?: sourceConfig.changeFilterID
        val filterConfig = if (filterConfigID != null) configuration.changeFilters[filterConfigID] else null
        // When there is no filter configuration a pass through filter is used
        filter = if (filterConfig != null) ChangeFilter(filterConfig) else PassThroughFilter

        // store and return the filter
        val channelFilters = filters.getOrPut(source) { mutableMapOf() }
        channelFilters[channel] = filter

        return filter
    }

    /**
     * Applies a channel or source level filter for a value. If no filter
     * @param source String Source ID
     * @param channel String Channel ID
     * @param value Any? Value to filter
     * @param logger Logger logger for errors
     * @return Pair<Boolean, Filter> True if passed filter, filter the applied filters
     */
    fun applyFilter(source: String, channel: String, value: Any, logger: Logger): Pair<Filter?, Boolean> {
        // Get and apply the filter for the source and channel
        val filter = getChangeFilter(source, channel)
        return try {
            val result = filter?.apply(value) ?: true
            filter to result
        } catch (ex: Exception) {
            logger.getCtxErrorLog(className, "applyFilter")("Error applying filter $filter to value $value (${value::class.java.simpleName})")
            filter to false
        }
    }

}