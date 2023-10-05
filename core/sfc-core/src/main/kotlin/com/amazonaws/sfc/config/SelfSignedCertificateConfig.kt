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

import com.google.gson.annotations.SerializedName
import java.time.Period

@ConfigurationClass
class SelfSignedCertificateConfig : Validate {

    @SerializedName(CONFIG_CERT_COMMON_NAME)
    private var _commonName: String = ""
    val commonName: String
        get() = _commonName

    @SerializedName(CONFIG_CERT_ORGANIZATION)
    private var _organization: String? = null
    val organization: String?
        get() = _organization

    @SerializedName(CONFIG_CERT_ORGANIZATIONAL_UNIT)
    private var _organizationalUnit: String? = null
    val organizationalUnit: String?
        get() = _organizationalUnit

    @SerializedName(CONFIG_CERT_LOCALITY_NAME)
    private var _localityName: String? = null
    val localityName: String?
        get() = _localityName

    @SerializedName(CONFIG_CERT_STATE_NAME)
    private var _stateName: String? = null
    val stateName: String?
        get() = _stateName

    @SerializedName(CONFIG_CERT_COUNTRY_CODE)
    private var _countryCode: String? = null
    val countryCode: String?
        get() = _countryCode

    @SerializedName(CONFIG_CERT_DNS_NAMES)
    private var _dnsNames: List<String>? = null
    val dnsNames: List<String>?
        get() = _dnsNames

    @SerializedName(CONFIG_CERT_IP_ADDRESSES)
    private var _ipAddresses: List<String>? = null
    val ipAddresses: List<String>?
        get() = _ipAddresses

    @SerializedName(CONFIG_CERT_VALIDITY_PERIOD_DAYS)
    private var _validityPeriodDays = CONFIG_CERT_DEFAULT_VALIDITY_PERIOD_DAYS
    val validityPeriodDays: Int
        get() = _validityPeriodDays

    private var _validated = false

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        validateCommonName()
        validatePeriod()
        validated = true
    }

    private fun validatePeriod() {
        ConfigurationException.check(
            _validityPeriodDays > 1,
            "$CONFIG_CERT_VALIDITY_PERIOD_DAYS must be more than 1 day",
            CONFIG_CERT_VALIDITY_PERIOD_DAYS,
            this
        )
    }

    private fun validateCommonName() {
        ConfigurationException.check(
            _commonName.isNotEmpty(),
            "$CONFIG_CERT_COMMON_NAME cannot be empty",
            CONFIG_CERT_COMMON_NAME,
            this
        )
    }


    companion object {


        private val default = SelfSignedCertificateConfig()

        @Suppress("unused")
        fun create(commonName: String = default._commonName,
                   organization: String? = default._organization,
                   organizationalUnit: String? = default._organizationalUnit,
                   localityName: String? = default._organizationalUnit,
                   stateName: String? = default._stateName,
                   countryCode: String? = default._countryCode,
                   dnsNames: List<String>? = default._dnsNames,
                   ipAddress: List<String>? = default._ipAddresses,
                   validityPeriodDays: Int = default._validityPeriodDays) {

            val instance = SelfSignedCertificateConfig()
            with(instance) {
                _commonName = commonName
                _organization = organization
                _organizationalUnit = organizationalUnit
                _localityName = localityName
                _stateName = stateName
                _countryCode = countryCode
                _dnsNames = dnsNames
                _ipAddresses = ipAddress
                _validityPeriodDays = validityPeriodDays
            }
        }


        private const val CONFIG_CERT_COMMON_NAME = "CommonName"
        private const val CONFIG_CERT_ORGANIZATION = "Organization"
        private const val CONFIG_CERT_ORGANIZATIONAL_UNIT = "OrganizationalUnit"
        private const val CONFIG_CERT_LOCALITY_NAME = "LocalityName"
        private const val CONFIG_CERT_STATE_NAME = "StateName"
        private const val CONFIG_CERT_COUNTRY_CODE = "CountryCode"
        private const val CONFIG_CERT_DNS_NAMES = "DnsNames"
        private const val CONFIG_CERT_IP_ADDRESSES = "IpAddresses"
        private const val CONFIG_CERT_VALIDITY_PERIOD_DAYS = "ValidPeriodDays"

        private val CONFIG_CERT_DEFAULT_VALIDITY_PERIOD_DAYS = Period.ofYears(3).days

    }
}

