/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ACTIVE
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.util.currentDirectory
import com.google.gson.annotations.SerializedName
import kotlin.io.path.Path
import kotlin.io.path.exists

class OpcuaCertificateValidationConfiguration : Validate {

    @SerializedName(CONFIG_ACTIVE)
    private var _active: Boolean = true
    val active: Boolean
        get() = _active

    @SerializedName(CONFIG_DIRECTORY)
    private var _directory: String? = null
    val directory: String
        get() {
            if (_directory == null) {
                _directory = currentDirectory()
            }
            return _directory as String
        }

    @SerializedName(CONFIG_VALIDATION_OPTIONS)
    private var _validationOptions: OpcuaCertificateValidationOptions = OpcuaCertificateValidationOptions()
    val configurationOptions: OpcuaCertificateValidationOptions
        get() = _validationOptions

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        ConfigurationException.check(
            Path(directory).exists(),
            "$CONFIG_DIRECTORY \"$directory\" base directory for certificates and certificate revocation lists does not exist",
            CONFIG_DIRECTORY,
            this
        )
    }

    companion object {
        private const val CONFIG_DIRECTORY = "Directory"
        private const val CONFIG_VALIDATION_OPTIONS = "ValidationOptions"

        private val default = OpcuaCertificateValidationConfiguration()

        fun create(
            directory: String? = default._directory,
            validationOptions: OpcuaCertificateValidationOptions = default._validationOptions): OpcuaCertificateValidationConfiguration {

            val instance = OpcuaCertificateValidationConfiguration()

            with(instance) {
                _directory = directory
                _validationOptions = validationOptions

            }
            return instance
        }
    }

}
