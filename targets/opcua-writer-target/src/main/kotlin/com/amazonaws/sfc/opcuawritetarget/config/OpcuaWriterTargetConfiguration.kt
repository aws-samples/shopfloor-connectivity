// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuawritetarget.config


import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.crypto.CertificateConfiguration
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.channel.MessageLimits
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class OpcuaWriterTargetConfiguration : TargetConfiguration() {

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_ADDRESS)
    private var _address = ""
    val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port = DEFAULT_PORT
    val port: Int
        get() = _port

    @SerializedName(CONFIG_PATH)
    private var _path = ""
    val path: String
        get() = _path

    val endPoint
        get() = listOf("${address}:${port}", path).joinToString(separator = "/")

    @SerializedName(CONFIG_NODES)
    val _nodes = mutableListOf<OpcuaNodeConfiguration>()
    val nodes: List<OpcuaNodeConfiguration>
        get() = _nodes

    @SerializedName(CONFIG_WRITE_BATCH_SIZE)
    private var _writeBatchSize: Int = 0
    val writeBatchSize: Int
        get() = if (_writeBatchSize !=0 )  _writeBatchSize else nodes.size


    @SerializedName(CONFIG_SECURITY_POLICY)
    private var _securityPolicy: OpcuaSecurityPolicy = OpcuaSecurityPolicy.None
    val securityPolicy: OpcuaSecurityPolicy
        get() = _securityPolicy

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WRITE_TIMEOUT)
    private var _writeTimeout: Long = DEFAULT_WRITE_TIMEOUT_MS
    val writeTimeout: Duration
        get() = _writeTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_MAX_MESSAGE_SIZE)
    private var _maxMessageSize: Int = DEFAULT_MAX_MESSAGE_SIZE
    val maxMessageSize: Int
        get() = _maxMessageSize

    @SerializedName(CONFIG_MAX_CHUNK_SIZE)
    private var _maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE
    val maxChunkSize: Int
        get() = _maxChunkSize

    @SerializedName(CONFIG_MAX_CHUNK_COUNT)
    private var _maxChunkCount: Int? = null
    val maxChunkCount: Int
        get() = _maxChunkCount ?: ((maxMessageSize / maxChunkSize) * 2)

    @SerializedName(CONFIG_CERTIFICATE)
    private var _certificateConfiguration: CertificateConfiguration? = null
    val certificateConfiguration: CertificateConfiguration?
        get() = _certificateConfiguration

    @SerializedName(CONFIG_CERTIFICATE_VALIDATION)
    private var _certificateValidationConfiguration: OpcuaCertificateValidationConfiguration? = null
    val certificateValidationConfiguration: OpcuaCertificateValidationConfiguration?
        get() = _certificateValidationConfiguration


    override fun validate() {
        super.validate()
        _certificateConfiguration?.validate()

        ConfigurationException.check(
            nodes.isNotEmpty(),
            "At least one node must be specified in $CONFIG_NODES",
            CONFIG_NODES,
            this)

        nodes.forEach { it.validate() }
        _certificateValidationConfiguration?.validate()
        _certificateConfiguration?.validate()

        validated = true
    }

    companion object {

        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_MAX_CHUNK_SIZE = MessageLimits.DEFAULT_MAX_CHUNK_SIZE
        const val DEFAULT_MAX_MESSAGE_SIZE = MessageLimits.DEFAULT_MAX_MESSAGE_SIZE
        const val DEFAULT_PORT = 53530
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        const val DEFAULT_WRITE_TIMEOUT_MS = 10000L

        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_WRITE_BATCH_SIZE = "BatchSize"
        private const val CONFIG_CERTIFICATE = "Certificate"
        private const val CONFIG_CERTIFICATE_VALIDATION = "CertificateValidation"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_MAX_CHUNK_COUNT = "MaxChunkCount"
        private const val CONFIG_MAX_CHUNK_SIZE = "MaxChunkSize"
        private const val CONFIG_MAX_MESSAGE_SIZE = "MaxMessageSize"
        private const val CONFIG_NODES = "Nodes"
        private const val CONFIG_PATH = "Path"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_SECURITY_POLICY = "SecurityPolicy"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WRITE_TIMEOUT = "WriteTimeout"

    }


}


