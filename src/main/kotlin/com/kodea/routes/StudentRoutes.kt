package com.kodea.routes

import com.kodea.config.UserPrincipal
import com.kodea.data.FileRepoImpl
import com.kodea.model.Role
import com.kodea.model.Student
import com.kodea.data.StudentRepoImpl
import com.kodea.model.StudentDTO
import com.kodea.model.toStudent
import com.kodea.utlis.Response
import com.kodea.utlis.generateToken
import com.kodea.utlis.hashedPassword
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.studentRoutes(
    studentService : StudentRepoImpl ,
    fileService : FileRepoImpl)
{

    post("/register") {
        val studentDTO = call.receive<StudentDTO>()
        if (studentService.readByEmail(studentDTO.email) != null) {
            call.respond(HttpStatusCode.BadRequest, "Email Already in Use")
            return@post
        }
        val id = studentService.register(studentDTO.copy(password = hashedPassword(studentDTO.password)).toStudent())
        val token = generateToken(id, Role.Student)
        //call.respondRedirect("/auth/signIn")
        call.respond(HttpStatusCode.Created, hashMapOf("token" to token , "id" to id))
    }
    @Serializable
    data class LoginRequest(val email: String, val password: String)
    post("/login") {
        try {
        val loginRequest = call.receive<LoginRequest>()
        if (loginRequest.email == "" || loginRequest.password == "") {
            call.respond(HttpStatusCode.BadRequest, "Email and Password are required")
            return@post
        }

        val resp = studentService.login(email = loginRequest.email, password = loginRequest.password)
        when(resp){
            is Response.Success -> call.respond(HttpStatusCode.OK , mapOf("token" to resp.data))
            is Response.Failure -> call.respond(HttpStatusCode.NotFound, mapOf("error" to resp.error))
        }
        }catch (e: Exception){
            call.respond(HttpStatusCode.BadRequest,mapOf("error" to e.message))
        }

    }


    authenticate("jwt-auth") {
        get("/studentDetails") {
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            studentService.read(id!!)?.let { student ->
                call.respondText(student , ContentType.Application.Json)
            } ?: call.respond(HttpStatusCode.NotFound , "student not found")
        }

        get("/wishlist"){
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            studentService.getWishlist(id!!)?.let { wishlist ->
                call.respondText(wishlist, ContentType.Application.Json)
            } ?: call.respond(HttpStatusCode.NotFound , "student not found")
        }
        post("wishlist/{id}/remove") {
            val params = call.parameters
            val courseId = params["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "course id is required"
            )
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            if (id != null) {
                val res= studentService.removeFromWishlist(id,courseId)
                if (res) {
                    call.respond(HttpStatusCode.OK, "Course removed from Wishlist.")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Course not found in wishlist.")
                }
            }
        }
        post("courses/{id}/addToWishlist") {
            val params = call.parameters
            val courseId = params["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "course id is required"
            )
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            if (id != null) {
                val res= studentService.addToWishlist(id,courseId)
                if (res) {
                    call.respond(HttpStatusCode.OK, "Course added Wishlist.")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Invalid course id.")
                }
            }
        }

        get("/cart"){
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            studentService.getCart(id!!)?.let { cart ->
                call.respondText(cart, ContentType.Application.Json)
            } ?: call.respond(HttpStatusCode.NotFound , "student not found")
        }
        post("cart/{id}/remove") {
            val params = call.parameters
            val courseId = params["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "course id is required"
            )
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            if (id != null) {
                val res= studentService.removeFromCart(id,courseId)
                if (res) {
                    call.respond(HttpStatusCode.OK, "Course removed from cart.")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Course not found in cart.")
                }
            }
        }

        post("courses/{id}/addToCart") {
            val params = call.parameters
            val courseId = params["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "course id is required"
            )
            val userPrincipal = call.authentication.principal<UserPrincipal>()
            val id = userPrincipal?.id
            if (id != null) {
                val res= studentService.addToCart(id,courseId)
                if (res) {
                    call.respond(HttpStatusCode.OK, "Course added cart.")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Invalid course id.")
                }
            }
        }
    }
    // Update student
    put("/students/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
        val student = call.receive<Student>()
        studentService.update(id, student)?.let {
            call.respond(HttpStatusCode.OK)
        } ?: call.respond(HttpStatusCode.NotFound)
    }

    //authenticate("jwt-auth") {
    get("/students") {
        //val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
        studentService.readAll().let { students ->
            call.respond(students)
        } ?: call.respond(HttpStatusCode.NotFound)
    }
}