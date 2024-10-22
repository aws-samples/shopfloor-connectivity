// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATE_VALIDATION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.crypto.CertificateConfiguration
import com.amazonaws.sfc.opcuatarget.config.DataModelConfigurationMap.Companion.DEFAULT_DATA_MODELS
import com.google.gson.annotations.SerializedName
import java.net.Inet4Address
import java.net.NetworkInterface


@ConfigurationClass
class OpcuaTargetConfiguration : TargetConfiguration() {

    @SerializedName(CONFIG_DATA_MODELS)
    private var _dataModels: DataModelConfigurationMap = DataModelConfigurationMap()
    val dataModels: DataModelConfigurationMap
        get() = _dataModels.ifEmpty { DEFAULT_DATA_MODELS }

    @SerializedName(CONFIG_DATA_MODEL_MAPPING)
    private var _datamodelMapping: Map<String, ScheduleMapping> = emptyMap()
    val datamodelMapping: Map<String, ScheduleMapping>
        get() = _datamodelMapping

    @SerializedName(CONFIG_AUTO_CREATE)
    private var _autoCreate: Boolean = true
    val autoCreate: Boolean
        get() = _autoCreate

    @SerializedName(CONFIG_SERVER_TCP_PORT)
    private var _serverTcpPort: Int = DEFAULT_SERVER_TCP_PORT
    val serverTcpPort: Int
        get() = _serverTcpPort

    @SerializedName(CONFIG_SERVER_PATH)
    private var _serverPath: String = DEFAULT_SERVER_PATH
    val serverPath: String
        get() = _serverPath.trim('/')

    @SerializedName(CONFIG_SERVER_ANONYMOUS_DISCOVERY_END_POINT)
    private var _anonymousDiscoveryEndPoint: Boolean = true
    val anonymousDiscoveryEndPoint: Boolean
        get() = _anonymousDiscoveryEndPoint

    @SerializedName(CONFIG_SERVER_NETWORK_INTERFACES)
    private var _serverNetworkInterfaces: List<String> = emptyList()
    val serverNetworkInterfaces: List<String>
        get() = _serverNetworkInterfaces.map { it.lowercase() }

    @SerializedName(CONFIG_SERVER_CERTIFICATE)
    private var _certificateConfiguration: CertificateConfiguration? = null
    val certificateConfiguration
        get() = _certificateConfiguration


    @SerializedName(CONFIG_CERTIFICATE_VALIDATION)
    private var _certificateValidationConfiguration: OpcuaCertificateValidationConfiguration = OpcuaCertificateValidationConfiguration()
    val certificateValidationConfiguration: OpcuaCertificateValidationConfiguration
        get() = _certificateValidationConfiguration


    @SerializedName(CONFIG_SERVER_MESSAGE_SECURITY_MODES)
    private var _serverMessageSecurityModes: List<String> = emptyList()
    val serverMessageSecurityModes: Set<OpcuaServerMessageSecurityMode>
        get() =
            if (_serverMessageSecurityModes.isEmpty()) DEFAULT_SERVER_SECURITY_MODES else
                _serverMessageSecurityModes.map { OpcuaServerMessageSecurityMode.fromString(it) }.filterNotNull().toSet()

    @SerializedName(CONFIG_SERVER_SECURITY_POLICIES)
    private var _serverSecurityPolicies: List<String> = emptyList()
    val serverSecurityPolicies: Set<OpcuaServerSecurityPolicy>
        get() =
            if (_serverSecurityPolicies.isEmpty()) {
                DEFAULT_SERVER_SECURITY_POLICIES
            } else
                _serverSecurityPolicies.map { OpcuaServerSecurityPolicy.fromString(it) }.filterNotNull().toSet()

    override fun validate() {
        super.validate()

        val invalidPolicies =_serverSecurityPolicies.filter { OpcuaServerSecurityPolicy.fromString(it) == null }
        if (invalidPolicies.isNotEmpty()){
            throw ConfigurationException("Invalid security policy(s) $invalidPolicies, valid modes are ${OpcuaServerSecurityPolicy.VALID_POLICIES}", CONFIG_SERVER_SECURITY_POLICIES)
        }

        val invalidModes =_serverMessageSecurityModes.filter { OpcuaServerMessageSecurityMode.fromString(it) == null }
        if (invalidModes.isNotEmpty()){
            throw ConfigurationException("Invalid security modes(s) $invalidModes, valid modes are ${OpcuaServerMessageSecurityMode.VALID_SECURTITY_MODES}", CONFIG_SERVER_MESSAGE_SECURITY_MODES)
        }

        val ip4networkInterfaces =  NetworkInterface.getNetworkInterfaces().toList().filter { it.inetAddresses.toList().any { a -> a is Inet4Address } }
        val invalidInterfaces = _serverNetworkInterfaces.filter { ni -> ip4networkInterfaces.none { it.name.equals(ni, ignoreCase = true) } }
        if (invalidInterfaces.isNotEmpty()){
            throw ConfigurationException("Invalid network interface(s) $invalidInterfaces, valid interfaces with IP4 addresses are ${ip4networkInterfaces.map { it.name }}", CONFIG_SERVER_NETWORK_INTERFACES)
        }

        if (serverSecurityPolicies.any { it != OpcuaServerSecurityPolicy.None }){
            ConfigurationException.check(
                _certificateConfiguration != null,
               "$CONFIG_SERVER_CERTIFICATE must be set for used $CONFIG_SERVER_SECURITY_POLICIES $_serverSecurityPolicies",
                CONFIG_SERVER_CERTIFICATE,
                this
            )
            certificateConfiguration?.validate()
        }


        validated = true
    }



    companion object {
        private const val CONFIG_DATA_MODELS = "DataModels"
        private const val CONFIG_DATA_MODEL_MAPPING = "DataModelMapping"
        private const val CONFIG_AUTO_CREATE = "AutoCreate"

        private const val CONFIG_SERVER_TCP_PORT = "ServerTcpPort"
        private const val DEFAULT_SERVER_TCP_PORT = 53530

        private const val CONFIG_SERVER_PATH = "ServerPath"
        private const val DEFAULT_SERVER_PATH = "sfc"

        private const val CONFIG_SERVER_ANONYMOUS_DISCOVERY_END_POINT = "ServerAnonymousDiscoveryEndPoint"

        private const val CONFIG_SERVER_NETWORK_INTERFACES = "ServerNetworkInterfaces"

        private const val CONFIG_SERVER_CERTIFICATE = "ServerCertificate"

        private const val CONFIG_SERVER_MESSAGE_SECURITY_MODES = "ServerMessageSecurityModes"
        private val DEFAULT_SERVER_SECURITY_MODES = setOf(OpcuaServerMessageSecurityMode.NONE, OpcuaServerMessageSecurityMode.SIGN, OpcuaServerMessageSecurityMode.SIGN_AND_ENCRYPT)
        private const val CONFIG_SERVER_SECURITY_POLICIES = "ServerSecurityPolicies"
        private val DEFAULT_SERVER_SECURITY_POLICIES =
            setOf(OpcuaServerSecurityPolicy.None, OpcuaServerSecurityPolicy.Basic128Rsa15, OpcuaServerSecurityPolicy.Basic256, OpcuaServerSecurityPolicy.Basic256Sha256)


    }
}


