
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config


import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_NAME
import com.google.gson.annotations.SerializedName

/**
 * Base class with minimum set of attributes for an SFC input source
 */
@ConfigurationClass
open class BaseSourceConfiguration : Validate {
    @SerializedName(CONFIG_NAME)
    @Suppress("PropertyName")
    protected var _name = ""

    /**
     * Name of the source
     */
    val name: String
        get() = _name


    @SerializedName(CONFIG_DESCRIPTION)
    @Suppress("PropertyName")
    protected var _description = ""

    /**
     * Description of the source
     */
    val description: String
        get() = _description

    private var validatedFlag = false
    override var validated
        get() = validatedFlag
        set(value) {
            validatedFlag = value
        }

    @SerializedName(CONFIG_SOURCE_PROTOCOL_ADAPTER)
    @Suppress("PropertyName")
    protected var _protocolAdapterID: String? = null
    val protocolAdapterID: String
        get() = _protocolAdapterID ?: ""

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateSourceName()
        validateMustHaveAdapter()
        validated = true
    }

    private fun validateSourceName() =
        ConfigurationException.check(
            (name.isNotBlank()),
            "Source $CONFIG_NAME can not be empty",
            CONFIG_NAME,
            this
        )

    private fun validateMustHaveAdapter() =
        ConfigurationException.check(
            (protocolAdapterID.isNotBlank()),
            "Source does not have protocol",
            CONFIG_SOURCE_PROTOCOL_ADAPTER,
            this
        )


    companion object {
        const val CONFIG_SOURCE_PROTOCOL_ADAPTER = "ProtocolAdapter"

        private val default = BaseSourceConfiguration()

        fun create(name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): BaseSourceConfiguration {

            val instance = BaseSourceConfiguration()
            with(instance) {
                _name = name
                _description = description
                _protocolAdapterID = protocolAdapter
            }
            return instance
        }

        @JvmStatic
        protected inline fun <reified T : BaseSourceConfiguration> createSourceConfiguration(name: String,
                                                                                             description: String = "",
                                                                                             protocolAdapter: String?): T {

            val parameterLessConstructor = T::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
            assert(parameterLessConstructor != null)
            val instance = parameterLessConstructor!!.newInstance() as T

            @Suppress("DuplicatedCode")
            with(instance) {
                _name = name
                _description = description
                _protocolAdapterID = protocolAdapter
            }

            return instance
        }


    }


}