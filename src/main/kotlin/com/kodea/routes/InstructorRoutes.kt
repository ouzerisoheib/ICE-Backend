package com.kodea.routes

import com.kodea.config.UserPrincipal
import com.kodea.data.InstructorRepoImpl
import com.kodea.model.Instructor
import com.kodea.model.Role
import com.kodea.utlis.generateToken
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.instructorRoutes(instructorService : InstructorRepoImpl) {
    authenticate("jwt-auth"){
        post("/be-instructor") {
            try {
                val id = call.principal<UserPrincipal>()?.id

                instructorService.beInstructor(id!!)
                call.respond(HttpStatusCode.Created , mapOf("token" to generateToken(id , Role.Instructor)))
            }catch (e : Exception){
                call.respond(HttpStatusCode.BadRequest , e.message ?:"error")
            }
        }
        post("/switchToInstructor") {
            val id = call.principal<UserPrincipal>()?.id
            call.respond(HttpStatusCode.Created , mapOf("token" to generateToken(id!!, Role.Instructor)))
        }
        get("/instructors") {

        }
        get("/instructors/{id}") {

        }
    }


}