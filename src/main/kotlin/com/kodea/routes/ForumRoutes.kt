package com.kodea.routes

import QuestionDTO
import com.kodea.config.UserPrincipal
import com.kodea.data.FileRepoImpl
import com.kodea.data.ForumRepoImpl
import com.kodea.model.AnswerDTO
import com.mongodb.client.MongoClient
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.forumRoutes(
    forumService: ForumRepoImpl,
    fileService: FileRepoImpl,
    mongoClient: MongoClient
){
    authenticate("jwt-auth") {
        post("/courses/{courseId}/forum") {
            val token = call.authentication.principal<UserPrincipal>()
            val userId = token?.id
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@post
            }

            val courseId = call.parameters["courseId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Course ID required")
            val questionDTO = call.receive<QuestionDTO>()

            val result = forumService.addQuestion(questionDTO, courseId, userId)
            if (result != null) {
                call.respond(HttpStatusCode.OK, "Question added successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Course not found")
            }
        }
        get("/courses/{courseId}/forum") {
            val courseId = call.parameters["courseId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Course ID required")
            val questions = forumService.getQuestions(courseId)
            call.respond(questions)
        }


        post("/forum/questions/{questionId}/answers") {
            val token = call.authentication.principal<UserPrincipal>()
            val role = token?.role
            val userId = token?.id ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            val questionId = call.parameters["questionId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Question ID required")
            val answerDTO = call.receive<AnswerDTO>()
            answerDTO.userRole = role!!
            val result = forumService.addAnswer(answerDTO, questionId, userId)
            call.respond(if (result != null) HttpStatusCode.OK else HttpStatusCode.NotFound)
        }

        get("/forum/questions/{questionId}/answers") {
            val questionId = call.parameters["questionId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Question ID required")
            val answers = forumService.getAnswers(questionId)
            call.respond(answers)
        }

        post("/forum/{type}/{id}/vote") {
            val token = call.authentication.principal<UserPrincipal>()
            val userId = token?.id ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            val type = call.parameters["type"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Type required")
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "ID required")
            val isUpvote = call.receive<Boolean>()
            val result = forumService.vote(type, id, userId, isUpvote)
            call.respond(if (result) HttpStatusCode.OK else HttpStatusCode.NotFound)
        }

        get("/forum/{type}/{id}/votes") {
            val type = call.parameters["type"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Type required")
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ID required")
            val votes = forumService.getVoteCount(type, id)
            call.respond(mapOf("votes" to votes))
        }

        post("/forum/questions/{id}/view") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "ID required")
            val result = forumService.incrementViews(id)
            call.respond(if (result) HttpStatusCode.OK else HttpStatusCode.NotFound)
        }

        get("/forum/questions/{id}/views") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ID required")
            val views = forumService.getViewCount(id)
            call.respond(mapOf("views" to views))
        }
    }
}