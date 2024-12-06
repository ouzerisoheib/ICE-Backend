package com.kodea.utlis

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kodea.model.Role
import io.ktor.server.application.*
import java.util.*

fun generateToken(id: String , role: Role) : String {
    val secretKey = "6673D2C5F86DD6E6F5F99AB6EF794"//environment.config.property("jwt.secretKey").getString()
    return JWT.create()
        //.withExpiresAt(Date(System.currentTimeMillis() + 3600000))
        .withClaim("id", id)
        .withClaim("role", role.name)
        .sign(Algorithm.HMAC256(secretKey))
}