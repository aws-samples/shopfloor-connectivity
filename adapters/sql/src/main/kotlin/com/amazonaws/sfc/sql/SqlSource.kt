/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.sql

import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.sql.config.DbServerConfiguration
import com.amazonaws.sfc.sql.config.SqlChannelConfiguration
import com.amazonaws.sfc.sql.config.SqlSourceConfiguration
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.Instant
import java.util.*

internal enum class SqlChannelValueType {
    SINGLE_COLUMN,
    MULTIPLE_COLUMNS,
    ALL_COLUMNS
}

class SqlSource(private val sourceID: String,
                private val dbServerID: String,
                private val dbServerConfiguration: DbServerConfiguration,
                private val sqlSourceConfiguration: SqlSourceConfiguration,
                private val metricsCollector: MetricsCollector?,
                adapterMetricDimensions: MetricDimensions?,
                private val logger: Logger) : Closeable {

    private val className = this::class.simpleName.toString()

    private var _connection: Connection? = null
    private var _statement: PreparedStatement? = null

    private var pausedUntil: Instant? = null
    private var badSourceReadStatement = false

    private var resultSet: ResultSet? = null

    private val protocolAdapterID = sqlSourceConfiguration.protocolAdapterID
    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>

    private val connection: Connection?
        get() {
            if (_connection != null) return _connection
            if (pausedUntil != null) {
                if (systemDateTime() < pausedUntil) {
                    return null
                }
                pausedUntil = null
            }

            _connection = runBlocking { createConnection() }

            return _connection
        }


    private suspend fun createConnection(): Connection {
        val log = logger.getCtxLoggers(className, "createConnection")

        return try {
            log.info("Creating connection to server \"$dbServerID\" (${dbServerConfiguration.databaseStr}) for reading for source \"$sourceID\"")
            val connection = SqlAdapter.connect(dbServerConfiguration)
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_CONNECTIONS, 1.0, MetricUnits.COUNT, sourceDimensions)
            connection
        } catch (e: TimeoutCancellationException) {
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
            throw Exception("Timeout connecting to server \"$dbServerID\" (${dbServerConfiguration.databaseStr}), ${e.message} for source \"$sourceID\"")
        } catch (e: Exception) {
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
            pausedUntil = systemDateTime().plusMillis(dbServerConfiguration.waitAfterConnectError.inWholeMilliseconds)
            throw Exception("Error connecting to server \"$dbServerID\" ${dbServerConfiguration.databaseStr}, ${e.message}, reading for source \"$sourceID\" paused for ${dbServerConfiguration.waitAfterConnectError} until $pausedUntil")
        }
    }


    private fun resetConnection() {
        try {
            close()
        } finally {
            _statement = null
            _connection = null
        }
    }

    private val statement: PreparedStatement?
        get() {
            if (_statement != null) return _statement!!

            if (badSourceReadStatement) return null

            if (connection == null) return null

            _statement = createSourceReadStatement()
            return _statement
        }

    private fun createSourceReadStatement(): PreparedStatement? {
        val log = logger.getCtxLoggers(className, "createSourceReadStatement")
        return try {
            log.trace("Creating prepared statement from \"${sqlSourceConfiguration.sqlReadStatement}\"")
            if (sqlSourceConfiguration.sqlReadParameters.isNotEmpty()) {
                log.trace("Using parameters ${sqlSourceConfiguration.sqlReadParameters}")
            }
            val st = connection?.prepareStatement(sqlSourceConfiguration.sqlReadStatement)
            setSqlParameters(st)
            st
        } catch (e: Exception) {
            _connection?.close()
            _connection = null
            badSourceReadStatement = true
            throw Exception("Error creating prepared statement from \"${sqlSourceConfiguration.sqlReadStatement}\" for source \"$sourceID\", ${e.message}")
        }
    }

    private fun setSqlParameters(st: PreparedStatement?) {
        sqlSourceConfiguration.sqlReadParameters.forEachIndexed { index, param ->
            val columnIndex = index + 1
            when (param) {
                is String -> st?.setString(columnIndex, param)
                is Int -> st?.setInt(columnIndex, param)
                is Byte -> st?.setByte(columnIndex, param)
                is Short -> st?.setShort(columnIndex, param)
                is Long -> st?.setLong(columnIndex, param)
                is Float -> st?.setFloat(columnIndex, param)
                is Date -> st?.setDate(columnIndex, java.sql.Date(param.toInstant().toEpochMilli()))
                is Double -> st?.setDouble(columnIndex, param)
                is Boolean -> st?.setBoolean(columnIndex, param)
                else -> throw Exception("Parameter type ${param::class.java} with value $param is not supported for a parameter $index in CONFIG_SOURCE_SQL_READ_PARAMS")
            }
        }
    }

    override fun close() {
        _statement?.close()
        _connection?.close()
    }


    private fun ResultSet.getAny(resultSet: ResultSet, baseType: Int, columnIndex: Int): Any? {

        return when (baseType) {
            Types.INTEGER -> resultSet.getInt(columnIndex)
            Types.VARCHAR -> resultSet.getString(columnIndex)
            Types.LONGVARCHAR -> resultSet.getString(columnIndex)
            Types.CHAR -> resultSet.getString(columnIndex)
            Types.BIT -> getBoolean(columnIndex)
            Types.TINYINT -> getByte(columnIndex)
            Types.SMALLINT -> getShort(columnIndex)
            Types.BIGINT -> getLong(columnIndex)
            Types.FLOAT -> getFloat(columnIndex)
            Types.REAL -> getFloat(columnIndex)
            Types.DOUBLE -> getDouble(columnIndex)
            Types.NUMERIC -> getBigDecimal(columnIndex).toDouble()
            Types.DECIMAL -> getBigDecimal(columnIndex).toDouble()
            Types.DATE -> getDate(columnIndex).toInstant()
            Types.TIME -> getTime(columnIndex).toInstant()
            Types.TIMESTAMP -> getTimestamp(columnIndex).toInstant()
            Types.BINARY -> getBinaryStream(columnIndex).readBytes()
            Types.VARBINARY -> getBinaryStream(columnIndex).readBytes()
            Types.LONGVARBINARY -> getBinaryStream(columnIndex).readBytes()
            Types.NULL -> null
            Types.OTHER -> getObject(columnIndex).toString()
            Types.JAVA_OBJECT -> getObject(columnIndex).toString()
            Types.DISTINCT -> getObject(columnIndex).toString()
            Types.STRUCT -> getObject(columnIndex).toString()
            Types.ARRAY -> getArrayValue(columnIndex)
            Types.BLOB -> getBlob(columnIndex).binaryStream.readBytes()
            Types.CLOB -> getClob(columnIndex).characterStream.readText()
            Types.REF -> getRef(columnIndex).getObject()
            Types.DATALINK -> getObject(columnIndex).toString()
            Types.BOOLEAN -> getBoolean(columnIndex)
            Types.ROWID -> getRowId(columnIndex).bytes.asList().toByteArray()
            Types.NCHAR -> getNCharacterStream(columnIndex).readText()
            Types.NVARCHAR -> getNCharacterStream(columnIndex).readText()
            Types.LONGNVARCHAR -> getNCharacterStream(columnIndex).readText()
            Types.NCLOB -> getNClob(columnIndex).characterStream.readText()
            Types.SQLXML -> getSQLXML(columnIndex).characterStream.readText()
            Types.REF_CURSOR -> getRef(columnIndex).getObject()
            Types.TIME_WITH_TIMEZONE -> getTime(columnIndex, Calendar.getInstance()).toInstant()
            Types.TIMESTAMP_WITH_TIMEZONE -> getTime(columnIndex, Calendar.getInstance()).toInstant()
            else -> null
        }

    }

    private fun ResultSet.getArrayValue(columnIndex: Int): MutableList<Any>? {
        val arrayValue = getArray(columnIndex) ?: return null
        val arrayValues = mutableListOf<Any>()
        val arrayBaseType = arrayValue.baseType
        val arrayResultSet = arrayValue.resultSet
        while (arrayResultSet.next()) {
            val elementValue = arrayResultSet.getAny(arrayResultSet, arrayBaseType, 2)
            if (elementValue != null) arrayValues.add(elementValue)
        }
        return arrayValues
    }

    private fun ResultSet.getAny(columnIndex: Int): Any? {
        val rs = resultSet ?: return null
        val type = columnsIndexToTypeMap[columnIndex] ?: return null
        return getAny(rs, type, columnIndex)
    }

    private val columnsIndexToTypeMap: Map<Int, Int> by lazy {

        val metaData = resultSet?.metaData
        sequence {
            for (i in 1..(metaData?.columnCount ?: 0)) {
                yield(i to (metaData?.getColumnType(i) ?: 0))
            }
        }.toMap()
    }

    private val columnsNameToIndexMap: Map<String, Int> by lazy {
        val metaData = resultSet?.metaData
        sequence {
            for (i in 1..(metaData?.columnCount ?: 0)) {
                val columnName = metaData?.getColumnName(i)
                if (columnName != null) yield(columnName.lowercase() to i)
            }
        }.toMap()
    }


    fun read(channels: List<String>?): Map<String, ChannelReadValue>? {

        val log = logger.getCtxLoggers(className, "read")

        if (statement == null) return null

        try {
            resultSet = statement!!.executeQuery()
            if (!resultSet?.next()!!) {
                log.trace("No rows returned for statement \"${sqlSourceConfiguration.sqlReadStatement}\" for source \"$sourceID\"")
                resultSet?.close()
                return null
            }

            val resultData: MutableMap<String, ChannelReadValue> = mutableMapOf()

            var rows = 0
            while (resultSet != null) {

                rows += 1

                sqlSourceConfiguration.channels.filter { channelFilter(it.key, channels) }.forEach { (channelID, channelConfig) ->

                    val rowData = getNativeRowData(channelConfig)
                    if (rowData != null) {
                        storeRowDataForChannel(resultData, channelID, rowData)
                    }
                }

                if (!resultSet!!.next() || sqlSourceConfiguration.singleRow) break
            }

            if (logger.level == LogLevel.TRACE) {
                log.trace("$rows row${if (rows == 1) "" else "s"} returned for statement \"${sqlSourceConfiguration.sqlReadStatement}\" for source \"$sourceID\"")
            } else {
                log.info("$rows row${if (rows == 1) "" else "s"} read from source \"$sourceID\"")
            }
            return resultData

        } catch (e: Exception) {
            pausedUntil = systemDateTime().plusMillis(dbServerConfiguration.waitAfterReadError.inWholeMilliseconds)
            resetConnection()
            log.error("Error reading data for source \"$sourceID\" using statement \"${sqlSourceConfiguration.sqlReadStatement}\", ${e.message}, reading from source will be paused for ${dbServerConfiguration.waitAfterReadError} until $pausedUntil")
            throw (e)
        } finally {
            resultSet?.close()
        }
    }

    private fun channelFilter(channelID: String, channels: List<String>?): Boolean {
        return channels.isNullOrEmpty() || channels.contains(channelID)
    }

    private fun getNativeRowData(channelConfig: SqlChannelConfiguration) =
        when (channelConfig.sqlValueType) {
            SqlChannelConfiguration.SqlChannelValueType.SINGLE_COLUMN -> {
                val values = columnValuesForRow(channelConfig.columnNames.toSet()).values
                if (values.isNotEmpty()) values.first() else null
            }

            SqlChannelConfiguration.SqlChannelValueType.ALL_COLUMNS -> columnValuesForRow(columnsNameToIndexMap.keys)
            SqlChannelConfiguration.SqlChannelValueType.MULTIPLE_COLUMNS -> columnValuesForRow(channelConfig.columnNames.toSet())
        }

    private fun storeRowDataForChannel(resultData: MutableMap<String, ChannelReadValue>, channelID: String, rowData: Any) {


        if (sqlSourceConfiguration.singleRow) {
            resultData[channelID] = ChannelReadValue(rowData)
        } else {
            val channelValue = resultData[channelID]
            if (channelValue == null) {
                resultData[channelID] = ChannelReadValue(mutableListOf(rowData))
            } else {
                @Suppress("UNCHECKED_CAST") // can only be a list of this type
                val valuesList = channelValue.value as MutableList<Any>
                valuesList.add(rowData)
            }
        }
    }

    private fun columnValuesForRow(columnNames: Set<String>): Map<String, Any?> = sequence {
        columnNames.map { columnName ->
            val columnIndex = columnsNameToIndexMap[columnName.lowercase()]
            if (columnIndex != null) {
                yield(columnName to resultSet!!.getAny(columnIndex))
            }
        }
    }.toMap()


}