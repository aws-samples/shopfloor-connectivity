/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ACTIVE
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class HealthProbeConfiguration : Validate {

    @SerializedName(CONFIG_PATH)
    private var _path = ""
    val path: String
        get() = _path

    @SerializedName(CONFIG_RATE_LIMIT)
    private var _rateLimit = CONFIG_DEFAULT_RATE_LIMIT
    val rateLimit: Int
        get() = _rateLimit

    @SerializedName(CONFIG_ALLOWED_IPS)
    private var _allowedIps: List<String>? = null
    val allowedIps: List<String>?
        get() = _allowedIps

    @SerializedName(CONFIG_PORT)
    private var _port: Int? = null
    val port: Int
        get() = _port ?: 0

    @SerializedName(CONFIG_NETWORK_INTERFACE)
    private var _networkInterface: String? = null
    val networkInterface: String?
        get() = _networkInterface

    @SerializedName(CONFIG_ACTIVE)
    private var _active: Boolean = true
    val active: Boolean
        get() = _active

    @SerializedName(CONFIG_OK_RESPONSE)
    private var _okResponse = CONFIG_DEFAULT_OK_RESPONSE
    val okResponse: String
        get() = _okResponse


    @SerializedName(CONFIG_RETAIN_STATE_PERIOD)
    private var _retainStatePeriod: Int = CONFIG_DEFAULT_RETAIN_STATE_PERIOD
    val retainStatePeriod: Duration
        get() = _retainStatePeriod.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_STOP_AFTER_UNHEALTY_PERIOD)
    private var _stopAfterUnhealthyPeriod: Int? = null
    val stopAfterUnhealthyPeriod: Duration?
        get() = _stopAfterUnhealthyPeriod?.toDuration(DurationUnit.SECONDS)


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

        validateRateLimit()
        validatePortNumber()

        validated = true
    }

    private fun validatePortNumber() {
        ConfigurationException.check(
            (port > 0),
            "$port is not a valid port number",
            CONFIG_PORT,
            this)
    }

    private fun validateRateLimit() {
        ConfigurationException.check(
            _rateLimit > 1,
            "$CONFIG_RATE_LIMIT must at least be 1",
            CONFIG_RATE_LIMIT,
            this
        )
    }


    companion object {
        private const val CONFIG_PATH = "Path"
        private const val CONFIG_RATE_LIMIT = "RateLimit"
        private const val CONFIG_NETWORK_INTERFACE = "Interface"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_OK_RESPONSE = "Response"
        private const val CONFIG_ALLOWED_IPS = "AllowedIpAddresses"
        private const val CONFIG_RETAIN_STATE_PERIOD = "RetainStatePeriod"
        private const val CONFIG_STOP_AFTER_UNHEALTY_PERIOD = "StopAfterUnhealthyPeriod"

        private const val CONFIG_DEFAULT_OK_RESPONSE = "OK"
        private const val CONFIG_DEFAULT_RATE_LIMIT = 10
        private const val CONFIG_DEFAULT_RETAIN_STATE_PERIOD = 1000

        private val default = HealthProbeConfiguration()

        fun create(path: String = default._path,
                   rateLimit: Int = default._rateLimit,
                   allowedIps: List<String>? = default._allowedIps,
                   okResponse: String = default._okResponse,
                   active: Boolean = default._active,
                   networkInterface: String? = default._networkInterface): HealthProbeConfiguration {

            val instance = HealthProbeConfiguration()
            with(instance) {
                _path = path
                _rateLimit = rateLimit
                _allowedIps = allowedIps
                _okResponse = okResponse
                _active = active
                _networkInterface = networkInterface
            }
            return instance
        }


    }

}