/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.csvfile.sfc.config

import com.amazonaws.csvfile.sfc.config.CsvFileConfiguration.Companion.CSVFILE_ADAPTER
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class CsvFileAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_CSV_FILES)
    private var _files = mapOf<String, CsvFileAdapterFileConfiguration>()

    val files: Map<String, CsvFileAdapterFileConfiguration>
        get() = _files

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        super.validate()
        files.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_CSV_FILES = "CsvFiles"

        private val default = CsvFileAdapterConfiguration()

        fun create(files: Map<String, CsvFileAdapterFileConfiguration> = default._files,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): CsvFileAdapterConfiguration {

            val instance = createAdapterConfiguration<CsvFileAdapterConfiguration>(
                description = description,
                adapterType = CSVFILE_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _files = files
            }

            return instance
        }
    }



}


