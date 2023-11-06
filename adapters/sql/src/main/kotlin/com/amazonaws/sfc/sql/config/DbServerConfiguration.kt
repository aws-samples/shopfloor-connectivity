
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class DbServerConfiguration : Validate {

    @SerializedName(CONFIG_DB_SERVER_HOST)
    private var _host = ""
    val host: String
        get() = _host

    @SerializedName(CONFIG_DB_SERVER_PORT)
    private var _port = 0
    val port: Int
        get() = _port

    @SerializedName(CONFIG_DB_TYPE)
    private var _dbServerType: DbServerType? = null

    val dbServerType: DbServerType?
        get() = _dbServerType

    @SerializedName(CONFIG_DB_NAME)
    private var _dbName: String = ""

    val dbName: String
        get() = _dbName

    @SerializedName(CONFIG_DB_USERNAME)
    private var _userName: String = ""

    val userName: String
        get() = _userName

    @SerializedName(CONFIG_DB_PASSWORD)
    private var _password: String = ""

    val password: String
        get() = _password

    @SerializedName(CONF_DB_INIT_SCRIPT)
    private var _initScript: String? = null

    val initScript: File?
        get() = if (!_initScript.isNullOrEmpty()) File(_initScript!!) else null

    @SerializedName(CONF_DB_INIT_SQL)
    private var _initSql: String? = null

    val initSql: String?
        get() = _initSql

    val databaseStr by lazy { "$host:$port/$dbName" }

    val jdbcConnectString by lazy {
        var connectString = dbServerType!!.connectString
        connectString = connectString.replace("\$host", host)
        connectString = connectString.replace("\$port", port.toString())
        connectString = connectString.replace("\$dbName", dbName)
        return@lazy connectString

    }
    //val jdbcConnectString by lazy{ "jdbc:${dbServerType.toString()}://$databaseStr"}

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError: Long = DEFAULT_WAIT_AFTER_READ_ERROR
    val waitAfterReadError: Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateHost()
        validatePort()
        validateDatabaseName()
        validateDatabaseType()
        validateConnectionTimeout()
        validateWaitAfterConnectError()
        validateInitScript()
        validated = true

    }

    private fun validateInitScript() {
        if (initScript != null) {
            ConfigurationException.check(
                initScript == null || initScript!!.exists(),
                "$CONF_DB_INIT_SCRIPT \"$initScript\" does not exist",
                CONF_DB_INIT_SCRIPT,
                this
            )
        }
    }

    private fun atLeastOneSecond(t: Duration) = t >= (1000.toDuration(DurationUnit.MILLISECONDS))


    private fun validateWaitAfterConnectError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterConnectError),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR must be 1000 (milliseconds) or longer",
            "WaitAfterConnectError",
            this
        )

    private fun validateConnectionTimeout() =
        ConfigurationException.check(
            atLeastOneSecond(connectTimeout),
            "Connect timeout $CONFIG_CONNECT_TIMEOUT must be 1000 (milliseconds) or longer",
            CONFIG_CONNECT_TIMEOUT,
            this
        )

    private fun validatePort() {
        ConfigurationException.check(
            (port > 0),
            "Port value not set or invalid",
            CONFIG_DB_SERVER_PORT,
            this
        )
    }

    private fun validateHost() =
        ConfigurationException.check(
            (host.isNotEmpty()),
            "Host of database server can not be empty",
            CONFIG_DB_SERVER_HOST,
            this
        )

    private fun validateDatabaseName() =
        ConfigurationException.check(
            (dbName.isNotEmpty()),
            "Database name can not be empty",
            CONFIG_DB_NAME,
            this
        )

    private fun validateDatabaseType() =
        ConfigurationException.check(
            (dbServerType != null),
            "Database type not set or invalid, valid types are ${DbServerType.values().joinToString()}",
            CONFIG_DB_TYPE,
            this
        )


    companion object {
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L

        private const val CONFIG_DB_SERVER_HOST = "Host"
        private const val CONFIG_DB_SERVER_PORT = "Port"
        private const val CONFIG_DB_TYPE = "DatabaseType"
        private const val CONFIG_DB_NAME = "DatabaseName"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_DB_USERNAME = "UserName"
        private const val CONFIG_DB_PASSWORD = "Password"
        private const val CONF_DB_INIT_SCRIPT = "InitScript"
        private const val CONF_DB_INIT_SQL = "InitSql"

        private val default = DbServerConfiguration()

        fun create(host: String = default._host,
                   port: Int = default._port,
                   databaseName: String = default._dbName,
                   databaseType: DbServerType? = default._dbServerType,
                   userName: String = default._userName,
                   password: String = default._password,
                   initScript: String? = default._initScript,
                   initSql: String? = default._initScript,
                   connectTimeout: Long = default._connectTimeout,
                   waitAfterConnectError: Long = default._waitAfterConnectError): DbServerConfiguration {

            val instance = DbServerConfiguration()
            with(instance) {
                _host = host
                _port = port
                _dbName = databaseName
                _dbServerType = databaseType
                _userName = userName
                _password = password
                _initScript = initScript
                _initSql = initSql
                _connectTimeout = connectTimeout
                _waitAfterConnectError = waitAfterConnectError
            }
            return instance
        }

    }
}