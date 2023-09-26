/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config

import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.util.InstanceFactory
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * Configuration for an in process input source reader or output target
 */
@ConfigurationClass
class InProcessConfiguration private constructor(jars: List<String> = emptyList(), classname: String = "") : Validate {

    @SerializedName(CONFIG_JAR_FILES)
    private var _jarFiles: List<String>? = jars
    val jarFiles: List<File>?
        get() = _jarFiles?.map { File(it.removeSuffix(File.separator)) }

    @SerializedName(CONFIG_FACTORY_CLASS_NAME)
    private var _factoryClassName: String = classname
    val factoryClassName: String
        get() = _factoryClassName

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        ConfigurationException.check(
            !jarFiles.isNullOrEmpty(),
            "$CONFIG_JAR_FILES can not be empty",
            CONFIG_JAR_FILES,
            this
        )

        jarFiles?.forEach { jar ->
            ConfigurationException.check(
                jar.exists(),
                "${jar.absolutePath} jar file does not exist",
                CONFIG_JAR_FILES,
                this
            )
        }

        ConfigurationException.check(
            InstanceFactory.expandedJarList(jarFiles ?: emptyList()).isNotEmpty(),
            "No jar files to load from ${jarFiles?.joinToString(separator = ",")}",
            CONFIG_JAR_FILES,
            this
        )

        ConfigurationException.check(
            factoryClassName.isNotBlank(),
            "$CONFIG_FACTORY_CLASS_NAME can not be empty",
            CONFIG_FACTORY_CLASS_NAME,
            this
        )

        validated = true
    }

    companion object {

        private fun getConfigSection(configStr: String, section: String): String? {
            // get config, which must be JSON but can can contain custom provider specific fields

            val configRaw = fromJsonExtended(configStr, Any::class.java) as Map<*, *>

            // Check for in-process config for custom provider
            if (!configRaw.containsKey(section)) {
                return null
            }
            val json = gsonExtended().toJson(configRaw[section])
            return ConfigReader.setEnvironmentValues(json)
        }

        fun getCustomConfig(configStr: String, customConfigName: String): InProcessConfiguration? {
            // replace environment variables in custom config configuration
            val customConfigAsJson: String = getConfigSection(configStr, customConfigName) ?: return null
            return fromMap(fromJsonExtended(customConfigAsJson, Map::class.java))
        }

        private fun fromMap(map: Map<*, *>): InProcessConfiguration? {
            return if (map.containsKey(CONFIG_JAR_FILES) && map[CONFIG_JAR_FILES] is List<*> && map.containsKey(CONFIG_FACTORY_CLASS_NAME))
                InProcessConfiguration(jars = (map[CONFIG_JAR_FILES] as List<*>).map { it as String }, classname = map[CONFIG_FACTORY_CLASS_NAME] as String)
            else null
        }

        const val CONFIG_JAR_FILES = "JarFiles"
        const val CONFIG_FACTORY_CLASS_NAME = "FactoryClassName"

        private val default = InProcessConfiguration()

        fun create(jarFiles: List<String> = default._jarFiles ?: emptyList(),
                   factoryClassName: String = default._factoryClassName): InProcessConfiguration {

            val instance = InProcessConfiguration()
            with(instance) {
                _jarFiles = jarFiles
                _factoryClassName = factoryClassName
            }
            return instance
        }
    }
}