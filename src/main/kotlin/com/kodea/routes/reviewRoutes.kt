package com.kodea.routes

import com.kodea.data.ReviewRepoImpl
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reviewRoute(reviewService : ReviewRepoImpl){
    get("/reviews/{id}"){
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest , "id required")
        try {
            reviewService.getReview(id)?.let {
                call.respondText(it , ContentType.Application.Json, HttpStatusCode.OK)
                return@get
            }
            call.respond(HttpStatusCode.NotFound , "review not found")

        }catch (e: Exception){
            call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
        }
    }
    get("/reviews"){
        val params = call.request.queryParameters
        val of = params["of"] ?: return@get call.respond(HttpStatusCode.BadRequest, "of parameter required")
        val id = params["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "id required")
        try {
            reviewService.getReviews(of , id)?.let {
                call.respondText(it , ContentType.Application.Json, HttpStatusCode.OK)
                return@get
            }
            call.respond(HttpStatusCode.NotFound, "review not found")
        }catch (e: Exception){
            call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
        }
    }
}