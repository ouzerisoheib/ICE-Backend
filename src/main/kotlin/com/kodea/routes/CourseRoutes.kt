package com.kodea.routes

import com.kodea.config.UserPrincipal
import com.kodea.model.CourseDTO
import com.kodea.model.toCourse
import com.kodea.data.CourseRepoImpl
import com.kodea.data.FileRepoImpl
import com.kodea.model.Role
import com.kodea.model.Section
import com.mongodb.client.MongoClient
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import org.bson.types.Binary

fun Route.courseRoutes(courseService: CourseRepoImpl, fileService: FileRepoImpl, mongoClient: MongoClient) {
    authenticate("jwt-auth") {
        post("/addSection/{courseId}"){
            val courseId = call.parameters["courseId"]?:return@post call.respond(HttpStatusCode.BadRequest , "course id required")
            val params = call.receive<Section>()
            try {
                val resp = courseService.addSection(courseId, params)?:""
                if (resp.isEmpty()) call.respond(HttpStatusCode.BadRequest, "not added")
                else call.respond(HttpStatusCode.OK, resp)

            }catch (e:Exception){
                call.respond(HttpStatusCode.BadRequest, e.message?:"unknown error")
            }
        }
        post("/createCourse"){
            val token = call.authentication.principal<UserPrincipal>()
            val id = token?.id
            val role = token?.role
            if (Role.Instructor.name != role) {
                call.respond(HttpStatusCode.Forbidden, "You don't have permission to create courses")
                return@post
            }
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@post
            }
            try {

                val multipart = call.receiveMultipart(1000 * 1024 * 1024)
                var courseDTO: CourseDTO? = null
                val filesIDs = arrayListOf<Pair<String, String>>()  // Holds fileName -> fileId
                var courseDuration = 0.0
                var courseImage: Binary? = null
                var courseTrailer: String = ""

                multipart.forEachPart {
                    when (it) {
                        is PartData.FormItem -> {
                            // Deserialize the course DTO
                            courseDTO = Json.decodeFromString<CourseDTO>(it.value)
                        }

                        is PartData.FileItem -> {
                            // Process file and get its ID
                            if (it.name == "courseImage") {
                                courseImage = Binary(it.provider().readRemaining().readByteArray())
                            } else if (it.name == "courseTrailer") {
                                val byteReadChannel = it.provider()
                                val inputStream = byteReadChannel.toInputStream()
                                val fileName = it.originalFileName ?: "unknown"
                                val contentType = it.contentType?.toString() ?: "application/octet-stream"
                                val (fileId, duration) = fileService.uploadFile(fileName, inputStream, contentType)
                                courseTrailer = fileId
                            }
                        }

                        else -> it.dispose()
                    }
                }

                if (courseDTO != null && courseImage != null) {
                    val newCourse = courseDTO!!.copy(
                        instructorId = id,
                        courseTrailer = courseTrailer
                    )

                    // Save the updated course
                    courseService.createCourse(newCourse.toCourse(), courseImage!!)
                    call.respond(HttpStatusCode.Created, "success")
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
            }

        }
        post("/course") {
            val token = call.authentication.principal<UserPrincipal>()
            val id = token?.id
            val role = token?.role
            if (Role.Instructor.name != role) {
                call.respond(HttpStatusCode.Forbidden, "You don't have permission to create courses")
                return@post
            }
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@post
            }
            try {

                val multipart = call.receiveMultipart(1000 * 1024 * 1024)
                var courseDTO: CourseDTO? = null
                val filesIDs = arrayListOf<Pair<String, String>>()  // Holds fileName -> fileId
                var courseDuration = 0.0
                var courseImage: Binary? = null
                var courseTrailer: String = ""

                multipart.forEachPart {
                    when (it) {
                        is PartData.FormItem -> {
                            // Deserialize the course DTO
                            courseDTO = Json.decodeFromString<CourseDTO>(it.value)
                        }

                        is PartData.FileItem -> {
                            // Process file and get its ID
                            if (it.name == "courseImage") {
                                courseImage = Binary(it.provider().readRemaining().readByteArray())
                            } else if (it.name == "courseTrailer") {
                                val byteReadChannel = it.provider()
                                val inputStream = byteReadChannel.toInputStream()
                                val fileName = it.originalFileName ?: "unknown"
                                val contentType = it.contentType?.toString() ?: "application/octet-stream"
                                val (fileId, duration) = fileService.uploadFile(fileName, inputStream, contentType)
                                courseTrailer = fileId
                            } else {
                                val byteReadChannel = it.provider()
                                val inputStream = byteReadChannel.toInputStream()
                                val fileName = it.originalFileName ?: "unknown"
                                val contentType = it.contentType?.toString() ?: "application/octet-stream"
                                val (fileId, duration) = fileService.uploadFile(fileName, inputStream, contentType)
                                duration?.let { videoDuration ->
                                    courseDuration += videoDuration
                                }

                                // Add the fileName and fileId pair to the list
                                filesIDs.add(fileName to fileId)
                            }
                        }

                        else -> it.dispose()
                    }
                }

                if (courseDTO != null && courseImage != null) {
                    val newCourse = courseDTO!!.copy(
                        sections = courseDTO!!.sections.asList().map { section ->
                            section.copy(
                                lectures = section.lectures.asList().map { lecture ->
                                    // Check the video field of the lecture and update it with the corresponding fileId
                                    val updatedVideo = filesIDs.find { it.first.lowercase().contains(lecture.name.lowercase()) }?.second
                                    lecture.copy(
                                        video = updatedVideo ?: lecture.video
                                    )  // If no match, keep original video
                                }.toTypedArray()
                            )
                        }.toTypedArray(),
                        duration = courseDuration,
                        instructorId = id,
                        courseTrailer = courseTrailer

                    )

                    // Save the updated course
                    courseService.addCourse(newCourse.toCourse(), courseImage!!)
                    call.respond(HttpStatusCode.Created, "success")
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
            }
        }
    }
    get("/courses/{id}") {
        try {
            val params = call.parameters
            val id = params["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "course id is required"
            )
            val fields = params["fields"]?.split(",")?.associateWith { 1 }
            val course = courseService.course(id, fields)
            if (course != null) {
                call.respondText(course, ContentType.Application.Json)
            } else call.respond(HttpStatusCode.NotFound, "course not found")

        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
        }
    }
    get("/courses") {
        try {
            val params = call.parameters
            val title = params["title"]
            val category = params["category"]
            val subCategory = params["subCategory"]
            val maxPrice = params["maxPrice"]?.toIntOrNull()
            val minPrice = params["minPrice"]?.toIntOrNull()
            val level = params["level"]?.split(",")
            val duration = params["duration"]?.toIntOrNull()
            val rating = params["rating"]?.toFloatOrNull()
            val fields = params["fields"]?.split(",")?.associateWith { 1 }
            val sortBy = params["sortBy"]
            val sortMap = sortBy?.split(",")?.map {
                val (field, order) = it.split(":")
                field to order.toInt()
            }?.toMap()
            val limit = params["limit"]?.toIntOrNull()
            val page = params["page"]?.toInt() ?: 1


            val filters = mutableMapOf<String, Any>()
            title?.let { filters["title"] = mapOf("\$regex" to it, "\$options" to "i") }
            category?.let { filters["category"] = it }
            subCategory?.let { filters["subCategory"] = it }
            if (minPrice != null && maxPrice != null) filters["price"] = mapOf("\$gte" to minPrice, "\$lte" to maxPrice)
            else if (minPrice != null && maxPrice == null) filters["price"] = mapOf("\$gte" to minPrice)
            else if (minPrice == null && maxPrice != null) filters["price"] = mapOf("\$lte" to maxPrice)
            rating?.let {
                filters["rating"] = mapOf("\$gte" to it)
            }
            level?.let { filters["level"] = mapOf("\$in" to it) }
            duration?.let { filters["duration"] = it }
            val result = courseService.courses(filters, fields, sortMap ?: mapOf("title" to 1), page, limit)
            val json = Json.parseToJsonElement(result.toString())
            call.respond(json)

        } catch (e: Exception) {
            call.respond(e.message ?: "error")
        }
    }
}