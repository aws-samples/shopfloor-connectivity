/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass

/**
 * Caches constructed value filters
 * @property filters Mapping<String, Filter?>
 * @constructor
 */
@ConfigurationClass
class ValueFiltersCache(filterConfigurations: Map<String, FilterConfiguration>) {
    val filters = filterConfigurations.map {
        it.key to FilterBuilder.build(it.value)
    }.filter { it.second != null }.toMap()

    operator fun get(key: String): Filter? = filters[key]

}