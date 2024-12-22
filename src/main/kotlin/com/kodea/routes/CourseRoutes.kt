package com.kodea.routes

import com.kodea.config.UserPrincipal
import com.kodea.data.CourseRepoImpl
import com.kodea.data.FileRepoImpl
import com.kodea.model.*
import com.mongodb.client.MongoClient
import com.mongodb.client.gridfs.GridFSBucket
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

fun Route.courseRoutes(
    courseService: CourseRepoImpl,
    fileService: FileRepoImpl,
    gridFSBucket: GridFSBucket,
    mongoClient: MongoClient
) {
    authenticate("jwt-auth") {
        post("/updateCourseImage/{id}") {
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
            val courseId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            val image = Binary(part.provider().readRemaining().readByteArray())
                            courseService.updateCourseImage(courseId, image)
                        }
                    }

                    else -> {
                        part.dispose()
                    }
                }
            }
        }
        post("/publicCourse") {
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
                val result = courseService.publicCourse(id)
                if (result) call.respond(HttpStatusCode.Created, "course published successfully")
                else call.respond(HttpStatusCode.NotFound, "course not found")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: " unknown error")
            }
        }
        /*


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

                }*/
        /*post("/course") {
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
        }*/
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
    /* get("/files/{id}") {
         val fileId = call.parameters["id"] ?: return@get call.respond(
             HttpStatusCode.BadRequest,
             "File name is required"
         )
         val file = fileService.getFile(fileId)
         if (file != null) {
             call.respondBytes(file)
         } else {
             call.respond(HttpStatusCode.NotFound, "File not found")
         }
     }*/
    get("/videos/{id}") {
        try {
            val fileId =
                call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "File name is required")
            val file = fileService.getVideo(fileId)
            if (file != null) {
                call.respondOutputStream(ContentType.Application.OctetStream) {
                    val downloadStream = gridFSBucket.openDownloadStream(file.id)

                    val buffer = ByteArray(1024 * 1024) // 1MB buffer
                    var bytesRead: Int
                    while (downloadStream.read(buffer).also { bytesRead = it } != -1) {
                        this.write(buffer, 0, bytesRead)
                    }
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found")
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error fetching video")
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
    authenticate("jwt-auth") {
        post("/courses/{courseId}/{sectionId}/addLecture") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "course id required")
            val sectionId =
                call.parameters["sectionId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "section id required"
                )
            val request = call.receive<Lecture>()

            val updateResult = courseService.addLecture(request, courseId, sectionId)

            if (updateResult != null) {
                call.respond(HttpStatusCode.OK, "Lecture added successfully.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Course or section not found.")
            }
        }

        delete("/courses/{courseId}/{sectionId}/{lectureId}/deletelecture") {
            val courseId = call.parameters["courseId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "course id required"
            )
            val sectionId = call.parameters["sectionId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "section id required"
            )
            val lectureId = call.parameters["lectureId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "lecture id required"
            )
            val deleteResult = courseService.deleteLecture(lectureId, courseId, sectionId)
            if (deleteResult) {
                call.respond(HttpStatusCode.OK, "Lecture deleted successfully.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Course or section or lecture not found.")
            }
        }
        post("/courses/{courseId}/addSection") {
            val courseId =
                call.parameters["courseId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "course id required")
            val params = call.receive<Section>()
            try {
                val resp = courseService.addSection(courseId, params) ?: ""
                if (resp.isEmpty()) call.respond(HttpStatusCode.BadRequest, "not added")
                else call.respond(HttpStatusCode.OK, resp)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "unknown error")
            }
        }
        delete("/courses/{courseId}/{sectionId}/deleteSection") {
            val courseId = call.parameters["courseId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "course id required"
            )
            val sectionId = call.parameters["sectionId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "section id required"
            )
            val deleteResult = courseService.deleteSection(courseId, sectionId)
            if (deleteResult) {
                call.respond(HttpStatusCode.OK, "section deleted successfully.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Course or section or lecture not found.")
            }
        }
    }


}