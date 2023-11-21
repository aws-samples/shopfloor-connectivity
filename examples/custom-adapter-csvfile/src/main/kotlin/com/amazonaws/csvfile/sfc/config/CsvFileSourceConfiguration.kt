/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.csvfile.sfc.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class CsvFileSourceConfiguration : BaseSourceConfiguration() {


    @SerializedName(CONFIG_CSV_FILE_ID)
    private var _adapterCsvFile: String? = null
    val adapterCsvFile: String?
        get() = _adapterCsvFile?.toString()

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, CsvFileChannelConfiguration>()
    val channels: Map<String, CsvFileChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    private fun validateCsvFileId() {
        ConfigurationException.check(
            !adapterCsvFile.isNullOrEmpty(),
            "${CONFIG_CSV_FILE_ID} can not be empty",
            CONFIG_CSV_FILE_ID,
            this
        )
    }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateCsvFileId()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }
        validated = true
    }

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "FILE source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {

        private const val CONFIG_CSV_FILE_ID = "AdapterCsvFile"

        private val default = CsvFileSourceConfiguration()

        fun create(name: String = default._name,
                   protocolAdapter: String? = default._protocolAdapterID,
                   adapterCsvFile: String? = default._adapterCsvFile,
                   channels: Map<String, CsvFileChannelConfiguration> = default._channels,
                   description: String = default._description
                   ): CsvFileSourceConfiguration {

            val instance = createSourceConfiguration<CsvFileSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)
            with(instance) {
                _adapterCsvFile = adapterCsvFile
                _channels = channels
            }
            return instance
        }


    }


}
