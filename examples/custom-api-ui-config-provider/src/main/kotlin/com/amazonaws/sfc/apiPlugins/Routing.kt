// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/sfc/editor.html#sfc-editor")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/sfc") {
            resources("static")
        }
    }
}
