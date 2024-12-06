package com.kodea.utlis

import com.kodea.model.Role
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

suspend fun PipelineContext<Unit, ApplicationCall>.checkRole(requiredRoles: Set<Role>) {
    val principal = call.principal<JWTPrincipal>()
    val userRole = principal?.getClaim("role", String::class)?.let { Role.valueOf(it) }

    if (userRole == null || userRole !in requiredRoles) {
        call.respond(HttpStatusCode.Forbidden, "Access Denied: Insufficient Permissions")
        finish() // Stops further processing
    }
}