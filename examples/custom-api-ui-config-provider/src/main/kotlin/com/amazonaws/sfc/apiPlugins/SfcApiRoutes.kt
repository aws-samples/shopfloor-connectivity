// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

import com.amazonaws.sfc.log.Logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.InetAddress


fun Application.sfcApiApp(ch: Channel<String>, log: Logger, writer: JsonElement, confProvider: JsonElement) {

    configureSerialization()
    configureRouting()
    configureSockets(log)
    val dbConnection: Connection = connectToPostgres(embedded = true)
    val configService = SfcConfigSchema(dbConnection)


    routing {
        // Create config
        post("/config") {
            val config = call.receive<SfcConfig>()
            val id = configService.create(config)
            call.respond(HttpStatusCode.Created, id)
        }

        // Push existing config to SFC-MAIN
        post("/push/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            try {
                val config = configService.read(id)
                // mark that conf/id as "Pushed"...
                configService.push(id)
                // Add our SocketLogWriter if not exists
                var confJson = Json.parseToJsonElement(config).jsonObject
                if (!confJson.containsKey("LogWriter")) {
                    confJson =  JsonObject(confJson + ("LogWriter" to writer) + ("ConfigProvider" to confProvider))
                }
                //print(Json.encodeToString(writer))
                // send config to SFC-MAIN
                ch.send(Json.encodeToString(confJson))
                call.respond(HttpStatusCode.Created, config)
            } catch (e: Exception) {
                log.error(this::class.simpleName + ":post" ,"Error in CFG", e)
                call.respond(HttpStatusCode.NotFound)
            }
        }


        // Get currently pushed config
        get("/pushed") {
            try {
                val config = configService.getPushed()
                call.respond(HttpStatusCode.OK, config)
            } catch (e: Exception) {
                e.stackTrace
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // get Hostname String
        get("/hostname") {
            try {
                val host = InetAddress.getLocalHost().hostName
                val ip = InetAddress.getLocalHost().hostAddress
                call.respond(HttpStatusCode.OK, "{\"hostname\":\"$host\",\"ip\":\"$ip\"}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Read config
        get("/config/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            try {
                val config = configService.read(id)
                call.respond(HttpStatusCode.OK, config)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // List config by id, name
        get("/config") {
            try {
                val config = configService.list()
                call.respond(HttpStatusCode.OK, config)
            } catch (e: Exception) {
                print(e.stackTrace)
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // Update config
        put("/config/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<SfcConfig>()
            configService.update(id, user)
            call.respond(HttpStatusCode.Created, user)
        }
        // Delete city
        delete("/config/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            configService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}


/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [SocketConnection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [SocketConnection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:file:./sfctest;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        DriverManager.getConnection(url, user, password)
    }
}
