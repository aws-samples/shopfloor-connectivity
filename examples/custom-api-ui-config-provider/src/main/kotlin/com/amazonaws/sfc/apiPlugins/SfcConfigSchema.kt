package com.amazonaws.sfc.apiPlugins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.Statement


@Serializable
data class SfcConfig(val name: String, val baseConfig: JsonObject)

class SfcConfigService(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE_CONFIGS =
            "CREATE TABLE IF NOT EXISTS CONFIGS (ID SERIAL PRIMARY KEY, NAME VARCHAR(255), CONFIG JSON);"
        private const val SELECT_CONFIG_BY_ID = "SELECT name, config FROM configs WHERE id = ?"
        private const val INSERT_CONFIG = "INSERT INTO configs (name, config) VALUES (?, ?::json)"
        private const val UPDATE_CONFIG = "UPDATE configs SET name = ?, config = ?::json WHERE id = ?"
        private const val DELETE_CONFIG = "DELETE FROM configs WHERE id = ?"

    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_CONFIGS)
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
    }

    // Delete a Config
    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(DELETE_CONFIG)
        statement.setInt(1, id)
        statement.executeUpdate()
    }
}
