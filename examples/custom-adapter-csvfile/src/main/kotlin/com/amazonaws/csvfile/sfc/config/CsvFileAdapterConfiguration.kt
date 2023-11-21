// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

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


