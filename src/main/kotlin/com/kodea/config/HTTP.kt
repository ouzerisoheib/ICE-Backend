package com.kodea.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/*fun Application.configureHTTP() {
    install(CORS) {
        *//*allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)*//*
        anyMethod()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost("localhost:5173")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
}*/
fun Application.configureHTTP() {
    install(CORS) {/*
        anyMethod() // Allow all HTTP methods (for dev only; restrict in production)
        allowHeader(HttpHeaders.Authorization) // Allow Authorization headers
        allowHeader(HttpHeaders.ContentType)   // Allow Content-Type headers
        allowHost("localhost:5173")     // Allow your Vue.js app
        allowHost("127.0.0.1:5173")     // (Optional) Allow using IP-based localhost
        allowCredentials = true               // Allow cookies and credentials
        */
        anyHost()
        anyMethod()
        allowHeader(HttpHeaders.Authorization) // Allow Authorization headers
        allowHeader(HttpHeaders.ContentType)   // Allow Content-Type headers
        //allowHeader(HttpHeaders.ContentType)
        //allowHeader(HttpHeaders.Range)
    }
}