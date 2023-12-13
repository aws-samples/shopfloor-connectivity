// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream


@Serializable
data class SfcConfig(val name: String, val baseConfig: JsonObject)

class SfcConfigService(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE_CONFIGS = "CREATE TABLE IF NOT EXISTS CONFIGS (ID SERIAL PRIMARY KEY, NAME VARCHAR(255), CONFIG JSON);"
        private const val CREATE_TABLE_PUSHED = "CREATE TABLE IF NOT EXISTS PUSHED (ID SERIAL PRIMARY KEY, CONFIG_ID INTEGER, FOREIGN KEY (CONFIG_ID) REFERENCES CONFIGS(id));"
        private const val SELECT_CONFIG_BY_ID = "SELECT name, config FROM configs WHERE id = ?"
        private const val SELECT_PUSHED = "SELECT config_id FROM pushed ORDER BY id DESC LIMIT 1"
        private const val INSERT_CONFIG = "INSERT INTO configs (name, config) VALUES (?, ?::json)"
        private const val UPDATE_CONFIG = "UPDATE configs SET name = ?, config = ?::json WHERE id = ?"
        private const val INSERT_PUSHED = "INSERT INTO pushed(config_id) VALUES (?)"
        private const val DELETE_CONFIG_LINKED = "DELETE FROM pushed WHERE config_id = ?"
        private const val DELETE_CONFIG = "DELETE FROM configs WHERE id = ?"
        private const val LIST_CONFIG = "SELECT id, name FROM configs"

    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_CONFIGS)
        statement.executeUpdate(CREATE_TABLE_PUSHED)
    }

    private var newConfigId = 0

    // Create new Config
    suspend fun create(config: SfcConfig): Int = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_CONFIG, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, config.name)
        val jsonObject = PGobject()
        jsonObject.setType("json");
        jsonObject.setValue(config.baseConfig.toString());
        statement.setObject(2, jsonObject.toString())
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted city")
        }
    }

    // Read a Config
    suspend fun read(id: Int): String = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_CONFIG_BY_ID)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            //val name = resultSet.getString("name")
            val baseConfig = resultSet.getString("config")
            return@withContext Json.decodeFromString(baseConfig)
        } else {
            throw Exception("Record not found")
        }
    }

    // list all Configs by id & name
    suspend fun list(): String = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(LIST_CONFIG)
        val resultSet = statement.executeQuery()
        val md = resultSet.metaData
        val numCols = md.columnCount
        val colNames: List<String> = IntStream.range(0, numCols)
            .mapToObj { i ->
                try {
                    return@mapToObj md.getColumnName(i + 1)
                } catch (e: SQLException) {
                    e.printStackTrace()
                    return@mapToObj "?"
                }
            }
            .collect(Collectors.toList())
        val result = buildJsonArray {
            while (resultSet.next()) {
                addJsonObject {
                    colNames.forEach(Consumer { cn: String ->
                        put(cn, resultSet.getObject(cn).toString())
                    })
                }
            }
        }
        return@withContext result.toString()
    }

    // Update a Config
    suspend fun update(id: Int, config: SfcConfig) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(UPDATE_CONFIG)
        statement.setString(1, config.name)
        val jsonObject = PGobject()
        jsonObject.setType("json");
        jsonObject.setValue(config.baseConfig.toString());
        statement.setObject(2, jsonObject.toString())
        statement.setInt(3, id)
        statement.executeUpdate()
        return@withContext(id)
    }

    // Push a Config
    suspend fun push(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_PUSHED)
        statement.setInt(1, id)
        statement.executeUpdate()
        return@withContext(id)
    }

    // GET currently PUSHED cfg
    suspend fun getPushed(): String = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_PUSHED)
        val resultSet = statement.executeQuery()
        if (resultSet.next()) {
            //val name = resultSet.getString("name")
            val pushed = resultSet.getString("config_id")
            return@withContext pushed
        } else {
            throw Exception("Record not found")
        }
    }

    // Delete a Config
    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val preDeleteStmnt = connection.prepareStatement(DELETE_CONFIG_LINKED)
        val statement = connection.prepareStatement(DELETE_CONFIG)
        preDeleteStmnt.setInt(1, id)
        preDeleteStmnt.executeUpdate()
        statement.setInt(1, id)
        statement.executeUpdate()
    }
}
