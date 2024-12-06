package com.kodea



import com.kodea.data.CourseRepoImpl
import com.kodea.data.StudentRepoImpl
import com.mongodb.client.gridfs.GridFSBucket
import io.ktor.server.application.*

fun Application.configureDatabases(studentService: StudentRepoImpl, courseService : CourseRepoImpl, gridFsBuckets: GridFSBucket) {
    /*val mongoDatabase = connectToMongoDB()
    val studentService = DatabaseService(mongoDatabase)
    val gridFSBucket: GridFSBucket = GridFSBuckets.insert(mongoDatabase)*/


    /*routing {

        get("/instructors") {
            val inst = studentService.getInstructors()
            if (inst.startsWith("error")) {
                call.respond(HttpStatusCode.BadRequest, inst)
            } else
                call.respond(HttpStatusCode.OK, inst)
        }

        get("/students/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            studentService.read(id)?.let { student ->
                call.respond(student)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Update student
        put("/students/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            val student = call.receive<Student>()
            studentService.update(id, student)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Delete student
        delete("/students/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            studentService.delete(id)?.let {
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



        post("/upload") {
            val multipart = call.receiveMultipart(100 * 1024 * 1024)
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val byteReadChannel = part.provider()
                        val inputStream = byteReadChannel.toInputStream()
                        val fileName = part.originalFileName ?: "unknown"
                        val contentType = part.contentType?.toString() ?: "application/octet-stream"

                        studentService.uploadFile(fileName, inputStream, contentType)
                        call.respond(HttpStatusCode.OK, "File uploaded successfully")
                    }

                    else -> part.dispose()
                }
            }
        }

        get("/files/{id}") {
            val fileId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "File name is required"
            )
            val file = studentService.getFile(fileId)
            if (file != null) {
                call.respondBytes(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found")
            }
        }

        post("/course") {
            try {
                val multipart = call.receiveMultipart(1000 * 1024 * 1024)
                var courseDTO: CourseDTO? = null
                val filesIDs = arrayListOf<Pair<String, String>>()  // Holds fileName -> fileId
                var courseDuration = 0.0

                multipart.forEachPart {
                    when (it) {
                        is PartData.FormItem -> {
                            // Deserialize the course DTO
                            courseDTO = Json.decodeFromString<CourseDTO>(it.value)
                        }

                        is PartData.FileItem -> {
                            // Process file and get its ID

                            val byteReadChannel = it.provider()
                            val inputStream = byteReadChannel.toInputStream()
                            val fileName = it.originalFileName ?: "unknown"
                            val contentType = it.contentType?.toString() ?: "application/octet-stream"
                            val (fileId , duration) = studentService.uploadFile(fileName, inputStream, contentType)
                            duration?.let { videoDuration ->
                                courseDuration += videoDuration
                            }

                            // Add the fileName and fileId pair to the list
                            filesIDs.add(fileName to fileId)
                        }

                        else -> it.dispose()
                    }
                }

                if (courseDTO != null) {
                    val newCourse = courseDTO!!.copy(
                        sections = courseDTO!!.sections.map { section ->
                            section.copy(
                                lectures = section.lectures.map { lecture ->
                                    // Check the video field of the lecture and update it with the corresponding fileId
                                    val updatedVideo = filesIDs.find { it.first == lecture.video }?.second
                                    lecture.copy(
                                        video = updatedVideo ?: lecture.video
                                    )  // If no match, keep original video
                                }
                            )
                        },
                        duration = courseDuration
                    )

                    // Save the updated course
                    studentService.createCourse(newCourse.toCourse())
                    call.respond(HttpStatusCode.Created, "success")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "error")
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
                val course = studentService.course(id, fields)
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
                val page = params["page"]?.toInt()?: 1




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
                val result =  studentService.courses(filters, fields, sortMap ?: mapOf("title" to 1) , page , limit)
                val json = Json.parseToJsonElement(result.toString())
                call.respond(json)

            } catch (e: Exception) {
                call.respond(e.message ?: "error")
            }
        }

    }*/
}


