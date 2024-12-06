package com.kodea.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.nio.file.attribute.UserPrincipal

fun Application.authentication() {

    val secretKey = "6673D2C5F86DD6E6F5F99AB6EF794"//environment.config.property("jwt.secretKey").getString()
    install(Authentication) {
        jwt("jwt-auth") {
            realm = "Ktor Server"  // Define your realm (optional)

            // Set up the JWT token verifier
            verifier(
                JWT.require(Algorithm.HMAC256(secretKey))  // Using HMAC algorithm and secret key for signing
                    .build()
            )

            // Define how to validate the token
            validate { credential ->
                val claimId = credential.payload.getClaim("id").asString()
                val claimRole = credential.payload.getClaim("role").asString()
                if (claimId != "" && claimRole != "") {
                    UserPrincipal(claimId , claimRole)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

}

data class UserPrincipal(val id: String, val role: String) : Principal