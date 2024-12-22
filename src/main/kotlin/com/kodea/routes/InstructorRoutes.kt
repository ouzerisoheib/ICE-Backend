package com.kodea.routes

import com.kodea.config.UserPrincipal
import com.kodea.data.CourseRepoImpl
import com.kodea.data.FileRepoImpl
import com.kodea.data.InstructorRepoImpl
import com.kodea.model.*
import com.kodea.utlis.generateToken
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
import org.bson.types.ObjectId

fun Route.instructorRoutes(
    instructorService: InstructorRepoImpl,
    courseService: CourseRepoImpl,
    fileService: FileRepoImpl
) {
    get("/instructors") {
        try {
            val instructors = instructorService.getInstructors()
            val json = Json.parseToJsonElement(instructors.toString())
            call.respond(json)

        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.message.toString() ?: "Unknown error")
        }

    }

    authenticate("jwt-auth") {
        post("/be-instructor") {
            try {
                val id = call.principal<UserPrincipal>()?.id
                instructorService.beInstructor(id!!)
                call.respond(HttpStatusCode.Created, mapOf("token" to generateToken(id, Role.Instructor)))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
            }
        }
        post("/switchToInstructor") {
            val id = call.principal<UserPrincipal>()?.id
            call.respond(HttpStatusCode.Created, mapOf("token" to generateToken(id!!, Role.Instructor)))
        }

        route("/instructor") {
            get{
                val token = call.authentication.principal<UserPrincipal>()
                val id = token?.id
                val role = token?.role
                if (Role.Instructor.name != role) {
                    call.respond(HttpStatusCode.Forbidden, "You don't have permission")
                    return@get
                }
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }
                val instructor = instructorService.getInstructor(id)
                instructor?.let {
                    call.respondText(instructor, ContentType.Application.Json)
                    return@get
                }
                call.respond(HttpStatusCode.NotFound, "instructor not found")

            }
            route("/courses") {
                get {
                    try {
                        val token = call.authentication.principal<UserPrincipal>()
                        val id = token?.id
                        val role = token?.role
                        if (Role.Instructor.name != role) {
                            call.respond(HttpStatusCode.Forbidden, "You don't have permission to create courses")
                            return@get
                        }
                        if (id.isNullOrBlank()) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                            return@get
                        }
                        call.respondText(
                            courseService.courses(filters = mapOf("instructorId" to id)).toString(),
                            ContentType.Application.Json
                        )
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Unknown error")
                    }
                }
                route("/draft") {

                    /**
                     * Create draft course with basic data
                     * @return the id of the course
                     */
                    post {
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
                                            val (fileId, duration) = fileService.uploadFile(
                                                fileName,
                                                inputStream,
                                                contentType
                                            )
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
                                instructorService.createCourse(id, newCourse.toCourse(), courseImage!!)
                                call.respond(HttpStatusCode.Created, "success")
                            }

                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
                        }

                    }
                    /**
                     * TODO("not implemented yet")
                     * @return basic information
                     */
                    get {
                        try {

                            val token = call.authentication.principal<UserPrincipal>()
                            val id = token?.id
                            val role = token?.role
                            if (Role.Instructor.name != role) {
                                call.respond(HttpStatusCode.Forbidden, "You don't have permission to create courses")
                                return@get
                            }
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                                return@get
                            }

                            call.respondText(
                                (instructorService.getCourse(id) ?: null).toString(),
                                ContentType.Application.Json
                            )
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, e.localizedMessage ?: "Unknown error")
                        }
                    }
                    route("/sections") {
                        /**
                         * TODO
                         * @return all sections
                         */
                        get {

                        }

                        /**
                         * Create new section
                         * @return id of the new section
                         */
                        post {
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
                            val params = call.receive<Section>()
                            try {
                                val resp = instructorService.addSection(id, params) ?: ""
                                if (resp.isEmpty()) call.respond(HttpStatusCode.BadRequest, "not added")
                                else call.respond(HttpStatusCode.OK, resp)

                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, e.message ?: "unknown error")
                            }
                        }
                        route("/{sectionId}") {

                            /**
                             * Delete single section
                             * @param sectionId
                             * @return Https status code
                             */
                            delete {
                                val token = call.authentication.principal<UserPrincipal>()
                                val id = token?.id
                                val role = token?.role
                                if (Role.Instructor.name != role) {
                                    call.respond(
                                        HttpStatusCode.Forbidden,
                                        "You don't have permission to create courses"
                                    )
                                    return@delete
                                }
                                if (id.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                                    return@delete
                                }
                                val sectionId = call.parameters["sectionId"] ?: return@delete call.respond(
                                    HttpStatusCode.BadRequest,
                                    "section id required"
                                )
                                val deleteResult = instructorService.deleteSection(id, sectionId)
                                if (deleteResult.modifiedCount > 0) {
                                    call.respond(HttpStatusCode.OK, "section deleted successfully.")
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Course or section or lecture not found.")
                                }
                            }
                            /**
                             * TODO(" not implemented yet")
                             * Update Section data
                             * @param title
                             * @param sectionId
                             */
                            put {

                            }
                            route("/lectures") {
                                /**
                                 * Create new lecture
                                 */
                                post {
                                    val token = call.authentication.principal<UserPrincipal>()
                                    val id = token?.id
                                    val role = token?.role
                                    if (Role.Instructor.name != role) {
                                        call.respond(
                                            HttpStatusCode.Forbidden,
                                            "You don't have permission to create courses"
                                        )
                                        return@post
                                    }
                                    if (id.isNullOrBlank()) {
                                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                                        return@post
                                    }
                                    val sectionId = call.parameters["sectionId"] ?: return@post call.respond(
                                        HttpStatusCode.BadRequest,
                                        "section id required"
                                    )
                                    var lectureDTO: LectureDTO? = null
                                    val multipart = call.receiveMultipart(1000 * 1024 * 1024)
                                    var video: String? = null
                                    var files = mutableListOf<Map<String, String>>()
                                    var duration: Double = 0.0
                                    multipart.forEachPart { part ->
                                        when (part) {
                                            is PartData.FormItem -> {
                                                lectureDTO = Json.decodeFromString<LectureDTO>(part.value)
                                            }

                                            is PartData.FileItem -> {
                                                if (part.name == "video") {
                                                    val byteReadChannel = part.provider()
                                                    val inputStream = byteReadChannel.toInputStream()
                                                    val fileName = part.originalFileName ?: "unknown"
                                                    val contentType =
                                                        part.contentType?.toString() ?: "application/octet-stream"
                                                    val (videoId, videoDuration) = fileService.uploadVideo(
                                                        fileName,
                                                        inputStream,
                                                        contentType
                                                    )
                                                    duration = videoDuration ?: 0.0
                                                    video = videoId
                                                } else if (part.name == "file") {
                                                    val byteReadChannel = part.provider()
                                                    val inputStream = byteReadChannel.toInputStream()
                                                    val fileName = part.originalFileName ?: "unknown"
                                                    val contentType =
                                                        part.contentType?.toString() ?: "application/octet-stream"
                                                    val documentId =
                                                        fileService.uploadDocument(fileName, inputStream, contentType)
                                                    files += mapOf("\$oid" to documentId)
                                                }
                                            }

                                            else -> part.dispose()
                                        }
                                    }
                                    if (lectureDTO != null && video != null) {

                                        val updateResult = instructorService.addLecture(
                                            id,
                                            lectureDTO!!.toLecture(video!!, files.toTypedArray(), duration),
                                            sectionId
                                        )

                                        if (updateResult.modifiedCount > 0) {
                                            call.respond(HttpStatusCode.OK, "Lecture added successfully.")
                                        } else {
                                            call.respond(HttpStatusCode.NotFound, "Course or section not found.")
                                        }
                                    } else call.respond(
                                        HttpStatusCode.NotModified,
                                        "ensure that all data it's correctly"
                                    )
                                }
                                /**
                                 * Todo("not implemented yet")
                                 * Delete single lecture
                                 */
                                delete("/{lectureId}") {
                                    val token = call.authentication.principal<UserPrincipal>()
                                    val id = token?.id
                                    val role = token?.role
                                    if (Role.Instructor.name != role) {
                                        call.respond(
                                            HttpStatusCode.Forbidden,
                                            "You don't have permission to create courses"
                                        )
                                        return@delete
                                    }
                                    if (id.isNullOrBlank()) {
                                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                                        return@delete
                                    }
                                    val sectionId = call.parameters["sectionId"] ?: return@delete call.respond(
                                        HttpStatusCode.BadRequest,
                                        "section id required"
                                    )
                                    val lectureId = call.parameters["lectureId"] ?: return@delete call.respond(
                                        HttpStatusCode.BadRequest,
                                        "lecture id required"
                                    )
                                    val deleteResult = instructorService.deleteLecture(id, lectureId, sectionId)
                                    if (deleteResult.modifiedCount > 0) {
                                        call.respond(HttpStatusCode.OK, "Lecture deleted successfully.")
                                    } else {
                                        call.respond(HttpStatusCode.NotFound, "Course or section or lecture not found.")
                                    }

                                }

                                put("/{lectureId}") {

                                }
                            }

                        }

                    }
                }
            }
        }
        post("/updateImage") {
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
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            val image = Binary(part.provider().readRemaining().readByteArray())
                            instructorService.updateInstructorImage(id, image)
                        }
                    }

                    else -> {
                        part.dispose()
                    }
                }
            }
        }

        get("/instructor/courses") {
            val token = call.authentication.principal<UserPrincipal>()
            val id = token?.id
            val role = token?.role
            if (Role.Instructor.name != role) {
                call.respond(HttpStatusCode.Forbidden, "You don't have permission to create courses")
                return@get
            }
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@get
            }
            val courses = courseService.courses(filters = mapOf("instructorId" to id))
            val json = Json.parseToJsonElement(courses.toString())
            call.respond(json)

        }

    }


}