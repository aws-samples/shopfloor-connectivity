/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import java.time.Instant

/**
 *
 * @property value Any? The value to output, could be a native, structured or a map/list of ChannelOutputData values
 * @property timestamp Instant? (optional) timestamp for the value
 * @property metadata Mapping<String, String>? (optional Metadata for the value
 * @constructor
 */
class ChannelOutputData(val value: Any?, val timestamp: Instant? = null, val metadata: Map<String, String>? = null) {

    /**
     * Custom method to display just the available data in compact format
     * @return String
     */
    override fun toString(): String {
        val ts = if (timestamp != null) ", timestamp=$timestamp" else ""
        val meta = if (!metadata.isNullOrEmpty()) ", metadata=$metadata" else ""
        return "(value=$value$ts$meta)"
    }

    fun toMap(elementNames: ElementNamesConfiguration, aggregated: Boolean = false): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        if (value != null) {
            map[elementNames.value] =
                if (aggregated)
                    ((value as? Map<*, *>)?.map {
                        it.key to (it.value as ChannelOutputData).toMap(elementNames)
                    }?.toMap()) as Any
                else
                    value
        }

        if (timestamp != null) {
            map[elementNames.timestamp] = timestamp
        }

        if (metadata != null) {
            map[elementNames.metadata] = metadata
        }

        return map
    }

}