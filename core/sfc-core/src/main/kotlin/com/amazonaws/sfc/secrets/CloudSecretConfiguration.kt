/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.secrets

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class CloudSecretConfiguration : Validate {

    @SerializedName(CONFIG_SECRET_ID)
    private var _id: String = ""
    val id: String
        get() = _id

    @SerializedName(CONFIG_SECRET_ALIAS)
    private var _alias: String = ""
    val alias: String
        get() = _alias

    @SerializedName(CONFIG_SECRET_LABELS)
    private var _labels: List<String>? = null
    val labels: List<String>?
        get() = _labels

    private var validatedFlag = false
    override var validated
        get() = validatedFlag
        set(value) {
            validatedFlag = value
        }

    fun asConfigurationMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(CONFIG_SECRET_ID to id)
        if (alias.isNotEmpty()) {
            map[CONFIG_SECRET_ALIAS] = alias
        }
        if (!labels.isNullOrEmpty()) {
            map[CONFIG_SECRET_LABELS] = labels
        }
        return map
    }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            (id.isNotEmpty()),
            "$CONFIG_SECRET_ID must contain a secret arn or name",
            CONFIG_SECRET_ID,
            this)

        validated = true
    }

    private fun parserNameFromArn(): String {
        val m = (ARN_PATTERN.find(_id))
        return m?.groups?.get(2)?.value ?: _id
    }

    val placeholderNames: Set<String>
        get() {
            val list: MutableSet<String> = if (alias.isNotEmpty()) mutableSetOf(alias) else mutableSetOf()
            list.add(parserNameFromArn())
            return list
        }


    companion object {
        const val CONFIG_SECRET_ID = "SecretId"
        const val CONFIG_SECRET_ALIAS = "Alias"
        const val CONFIG_SECRET_LABELS = "Labels"

        private const val VALID_SECRET_ARN_PATTERN =
            ("arn:([^:]+):secretsmanager:[a-z0-9\\-]+:[0-9]{12}:secret:(([a-zA-Z0-9\\\\]+/)*[a-zA-Z0-9/_+=,.@\\-]+)-[a-zA-Z0-9]+")

        val ARN_PATTERN = Regex(VALID_SECRET_ARN_PATTERN)

        private val default = CloudSecretConfiguration()

        fun create(secretId: String = default._id,
                   alias: String = default._alias,
                   labels: List<String>? = default._labels): CloudSecretConfiguration {

            val instance = CloudSecretConfiguration()
            with(instance) {
                _id = secretId
                _alias = alias
                _labels = labels
            }
            return instance
        }
    }

    override fun equals(other: Any?): Boolean =
        if (other is CloudSecretConfiguration) {
            (_id == other._id)
        } else false

    override fun hashCode(): Int {
        return _id.hashCode()
    }


}