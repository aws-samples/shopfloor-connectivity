// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CACHE_URL_CONFIG_CACHE_DIRECTORY
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CACHE_URL_CONFIG_CACHE_RESULTS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TEMPLATES
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonPretty
import com.amazonaws.sfc.filters.*
import com.amazonaws.sfc.secrets.SecretsManager
import com.amazonaws.sfc.transformations.TransformationOperator
import com.amazonaws.sfc.transformations.TransformationsDeserializer
import com.amazonaws.sfc.util.currentDirectory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.File


/**
 * Abstraction for reading configuration data of a specified type from a JSON source
 * @property config String JSON source data
 */
open class ConfigReader(val config: String, val allowUnresolved: Boolean = false, val secretsManager: SecretsManager? = null) {

    val jsonConfig = processConfig(config)

    val usedSecrets = mutableMapOf<String, String>()

    /**
     * Gets the configuration for type T from the configuration
     * @param validate Boolean If set to true the read configuration will be validated by calling the validate method of type T. If the configuration is invalid a ConfigurationException will be thrown
     * @return T Configuration of type T read from the configuration data
     * @see ConfigurationException
     */
    inline fun <reified T : Validate> getConfig(validate: Boolean = true): T {

        return try {
            val config = fromJsonExtended(jsonConfigReader, jsonConfig, T::class.java)
            if (validate) {
                config.validate()
            }
            config
        } catch (e: Throwable) {
            val s = T::class.simpleName?.substringAfter(".") ?: ""
            val ee = if (e is JsonSyntaxException) e.extendedJsonException(jsonConfig) else e
            throw ConfigurationException("Error getting configuration of type $s: $ee", s)
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
    @Suppress("UNCHECKED_CAST")
    private fun processConfig(configString: String): String {

        val configWithIncluded = includeFiles(configString).toString()

        val configMap = fromJsonExtended(configWithIncluded, Map::class.java) as Map<String, Any>
        val useCachedResults = try {
            (configMap[CONFIG_CACHE_URL_CONFIG_CACHE_RESULTS] as Boolean?) == true
        } catch (_: Throwable) {
            false
        }
        val cacheDirectory = try {
            (configMap[CONFIG_CACHE_URL_CONFIG_CACHE_DIRECTORY] as String?)
        } catch (_: Throwable) {
            currentDirectory()
        }
        IncludeResolver.cacheResults = useCachedResults
        IncludeResolver.cacheDirectory = cacheDirectory
        val included = IncludeResolver.resolve(configMap) as Map<String, Any>
        val resolved = TemplateResolver(CONFIG_TEMPLATES).resolve(included)
        var configStr = gsonPretty().toJson(resolved)
        return if  (CONFIG_PLACEHOLDER_REGEX.find(configStr) != null)
            setPlaceholders(configStr)
        else configStr
    }



    private fun setPlaceholders(inputStr: String): String {
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
            throw ConfigurationException(
                "Placeholder$s $unresolved could not be replaced as there ${if (single) "is" else "are"} no environment variable$s $m available with $t name$s",
                ""
            )
        }
    }


    private fun setSecretValues(config: String): String {
        if (secretsManager == null) return config
        var configOut = config
        getPlaceHolders(config).forEach {
            val secretIdOrAlias = it.groups[1]?.value
            if (!secretIdOrAlias.isNullOrEmpty() && (secretsManager.secrets.containsKey(secretIdOrAlias) || secretsManager.secrets.containsValue(secretIdOrAlias))) {
                val secretValue = secretsManager.getSecret(secretIdOrAlias).secretString().trim('\'', '\"')
                configOut = configOut.replace(it.groups[0]!!.value, secretValue)
                usedSecrets[secretIdOrAlias] = secretValue
            }
        }
        return configOut
    }

    protected open fun createJsonConfigReader(): Gson = GsonBuilder()
        .registerTypeAdapter(ValueFilterConfiguration::class.java, FilterConfigurationDeserializer())
        .registerTypeAdapter(ValueFilterConfiguration::class.java, ValueFilterConfigurationDeserializer())
        .registerTypeAdapter(ConditionFilterConfiguration::class.java, ConditionFilterConfigurationDeserializer())
        .registerTypeAdapter(TransformationOperator::class.java, TransformationsDeserializer())
        .create()


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


        // // Creates reader for a configuration string
        fun createConfigReader(configStr: String, allowUnresolved: Boolean = false, secretsManager: SecretsManager? = null): ConfigReader {
            return ConfigReader(configStr, allowUnresolved, secretsManager)
        }

        private const val PLACEHOLDER_PATTERN = """\$\{\s*([a-zA-Z0-9\-_:/]+)\s*}"""

        private val CONFIG_PLACEHOLDER_REGEX = Regex(PLACEHOLDER_PATTERN)
        private val EXTERNAL_CONFIG_PLACEHOLDER_REGEX = Regex(PLACEHOLDER_PATTERN.replace("\\{", "\\{\\{").replace("}", "}}"))
        private val CONFIG_INCLUDE = "@include"
        private val CONFIG_INCLUDE_FILE = Regex(""""$CONFIG_INCLUDE"?\s*:\s*"?(.*)"\s*,?""")

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

        fun getIncludedItems(configString: String): List<String> {
            var s = configString
            var includedItems = mutableSetOf<String>()
            var done = false

            while(!done) {
                s = includeFiles(s){includedItems.add("file:$it")}
                val configMap = fromJsonExtended(s, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                val resolved = IncludeResolver.resolve(configMap, fnResolved = { l -> includedItems.addAll(l) }) as Map<String,Any>
                s = gsonPretty().toJson(resolved)
                done = CONFIG_INCLUDE_FILE.containsMatchIn(s) == false
            }
            return includedItems.toList()
        }

        private fun includeFiles(configString: String, trace: List<String> = emptyList<String>(),  fn : (s : String) -> Unit = {}) : String {

            var s = configString

            var match = CONFIG_INCLUDE_FILE.find(s)

            while (match != null) {

                val includedFileName = match.groups[1]?.value

                if (includedFileName == null) throw ConfigurationException("${match.value} does not specify a file name", CONFIG_INCLUDE)

                val includeFile = File(includedFileName)

                fn(includedFileName)

                if (includeFile.absolutePath in trace) throw ConfigurationException(
                    "Recursive inclusion found for file ${includeFile.absolutePath}, ${trace.joinToString(separator = "->")}",
                    CONFIG_INCLUDE)

                if (!(includeFile.exists() && includeFile.canRead())) throw ConfigurationException("Included file \"${includeFile.absolutePath}\" does not exist or can not be read", CONFIG_INCLUDE)
                try {
                    val includedLines = includeFile.readText()
                    val includedContent = if (includedLines.contains(CONFIG_INCLUDE)) {
                        includeFiles(includedLines, trace + includeFile.absolutePath)
                        includedLines
                    } else
                        includedLines


                    s = s.replace(match.value, includedContent)
                } catch (e: Exception) {
                    throw ConfigurationException("Error reading included file \"${includeFile.absolutePath}\", $e", CONFIG_INCLUDE)
                }

                match = CONFIG_INCLUDE_FILE.find(s)
            }
            return s
        }

    }
}