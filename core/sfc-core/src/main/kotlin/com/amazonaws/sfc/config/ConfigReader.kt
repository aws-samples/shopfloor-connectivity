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

import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.filters.FilterConfiguration
import com.amazonaws.sfc.filters.FilterConfigurationDeserializer
import com.amazonaws.sfc.secrets.SecretsManager
import com.amazonaws.sfc.transformations.TransformationOperator
import com.amazonaws.sfc.transformations.TransformationsDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder


/**
 * Abstraction for reading configuration data of a specified type from a JSON source
 * @property config String JSON source data
 */
class ConfigReader private constructor(val config: String, private val allowUnresolved: Boolean = false, val secretsManager: SecretsManager? = null) {

    val jsonConfig by lazy { processConfig() }

    val usedSecrets = mutableMapOf<String, String>()

    /**
     * Gets the configuration for type T from the configuration
     * @param validate Boolean If set to true the read configuration will be validated by calling the validate method of type T. If the configuration is invalid a ConfigurationException will be thrown
     * @return T Configuration of type T read from the configuration data
     * @see ConfigurationException
     */
    inline fun <reified T : Validate> getConfig(validate: Boolean = true): T {

        return try {
            val config = jsonConfigReader.fromJson(jsonConfig, T::class.java)
            if (validate) {
                config.validate()
            }
            config
        } catch (e: Throwable) {
            val s = T::class.simpleName?.substringAfter(".") ?: ""
            throw ConfigurationException("Error getting configuration of type $s: $e", s)
        }
    }

    val jsonConfigReader: Gson = createJsonConfigReader()


    /**
     * Configuration data as a string
     * @return String
     */
    override fun toString(): String {
        return jsonConfig
    }

    // replaces environment variable placeholders in configuration
    private fun processConfig(): String {
        return setPlaceholders(config)
    }

    fun setPlaceholders(inputStr: String): String {
        var outputStr = setEnvironmentValues(inputStr)
        outputStr = setSecretValues(outputStr)
        if (!allowUnresolved) {
            checkReplacements(outputStr)
        }
        return outputStr
    }

    private fun checkReplacements(configOut: String) {
        val unresolved = getPlaceHolders(configOut).map { it.groups[1]?.value }.filterNotNull().toList()
        if (unresolved.isNotEmpty()) {
            val single = unresolved.size == 1
            val s = if (!single) "s" else ""
            val m = if (secretsManager != null) "or configured secrets " else ""
            val t = if (single) "that" else "these"
            throw ConfigurationException("Placeholder$s $unresolved could not be replaced as there ${if (single) "is" else "are"} no environment variable$s $m available with $t name$s", "")
        }
    }


    private fun setSecretValues(config: String): String {
        if (secretsManager == null) return config
        var configOut = config
        getPlaceHolders(config).forEach {
            val secretIdOrAlias = it.groups[1]?.value
            if (!secretIdOrAlias.isNullOrEmpty() && secretsManager.secrets.containsKey(secretIdOrAlias)) {
                val secretValue = secretsManager.getSecret(secretIdOrAlias).secretString().trim('\'', '\"')
                configOut = configOut.replace(it.groups[0]!!.value, secretValue)
                usedSecrets[secretIdOrAlias] = secretValue
            }
        }
        return configOut
    }

    companion object {


        fun setEnvironmentValues(config: String): String {
            var configOut = config
            getPlaceHolders(config).forEach {
                val envVariableName = it.groups[1]?.value
                val envVariableValue = System.getenv(envVariableName)
                if (envVariableValue != null) {
                    configOut = configOut.replace(it.groups[0]!!.value, envVariableValue.trim('\'', '\"'))
                }
            }
            return configOut
        }

        fun getExternalPlaceHolders(config: String): List<String> =
            EXTERNAL_CONFIG_PLACEHOLDER_REGEX.findAll(config).map { it.groups[1]?.value ?: "" }.toList()

        fun getPlaceHolders(config: String) = CONFIG_PLACEHOLDER_REGEX.findAll(config)

        private fun createJsonConfigReader() = GsonBuilder()
            .registerTypeAdapter(FilterConfiguration::class.java, FilterConfigurationDeserializer())
            .registerTypeAdapter(TransformationOperator::class.java, TransformationsDeserializer())
            .create()

        // // Creates reader for a configuration string
        fun createConfigReader(configStr: String, allowUnresolved: Boolean = false, secretsManager: SecretsManager? = null): ConfigReader {
            return ConfigReader(configStr, allowUnresolved, secretsManager)
        }

        private const val PLACEHOLDER_PATTERN = "\\\$\\{\\s*([a-zA-Z\\-0-9_]+)\\s*}"

        private val CONFIG_PLACEHOLDER_REGEX = Regex(PLACEHOLDER_PATTERN)
        private val EXTERNAL_CONFIG_PLACEHOLDER_REGEX = Regex(PLACEHOLDER_PATTERN.replace("\\{", "\\{\\{").replace("}", "}}"))

        fun convertExternalPlaceholders(strIn: String): String {
            var strOut = strIn
            EXTERNAL_CONFIG_PLACEHOLDER_REGEX.findAll(strIn).forEach {
                strOut = strOut.replace((it.groups[0]?.value) ?: "", "\${${it.groups[1]?.value}}")
            }
            return strOut
        }


        private fun parsePlaceHolders(nodeMap: Map<*, *>): List<Pair<String, String>> {
            val map = mutableListOf<Pair<String, String>>()
            nodeMap.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    map.addAll(parsePlaceHolders(v))
                } else {
                    if (v != null) {
                        val match = CONFIG_PLACEHOLDER_REGEX.find(v.toString())
                        if (match != null) {
                            map.add(k.toString() to (match.groups[1]?.value ?: ""))
                        }
                    }
                }
            }
            return map
        }

        fun parsePlaceHolders(configStr: String): List<Pair<String, String>> {

            val configAsMap = fromJsonExtended(configStr, Any::class.java) as Map<*, *>
            return parsePlaceHolders(configAsMap)
        }
    }
}