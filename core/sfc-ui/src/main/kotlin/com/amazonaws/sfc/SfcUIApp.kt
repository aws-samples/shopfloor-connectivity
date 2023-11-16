package com.amazonaws.sfc

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import com.amazonaws.sfc.sfcui.plugins.*

fun main() {

    embeddedServer(Tomcat, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    configureSerialization()
    configureDatabases()
    configureRouting()
}
