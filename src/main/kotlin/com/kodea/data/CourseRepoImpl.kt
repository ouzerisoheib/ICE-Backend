package com.kodea.data


import com.kodea.model.Course
import io.ktor.server.application.*
import com.kodea.model.Lecture
import com.kodea.model.Section
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.*
import org.bson.conversions.Bson
import org.bson.types.Binary
import org.bson.types.ObjectId
import java.util.Base64
import com.kodea.utlis.getVideoDuration
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UnwindOptions
import com.mongodb.client.model.UpdateOptions

import java.io.File
import java.util.logging.Filter

class CourseRepoImpl(private val database: MongoDatabase) {
    private var coursesCollection: MongoCollection<Document>
    private var studentsCollection: MongoCollection<Document>
    private var instructorCollection: MongoCollection<Document>

    init {
        database.createCollection("courses")
        coursesCollection = database.getCollection("courses")
        instructorCollection = database.getCollection("users")
        studentsCollection = database.getCollection("users")

    }


    /*suspend fun addCourse(course: Course, courseImage: Binary): ObjectId? {
        return withContext(Dispatchers.IO) {
            val result = coursesCollection.insertOne(course.toDocument().append("courseImage", courseImage))
            result.insertedId?.asObjectId()?.value
        }
    }*/

    suspend fun course(id: String, fields: Map<String, Int>?) = withContext(Dispatchers.IO) {

        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(Filters.eq(ObjectId(id)))
        pipeline += Aggregates.lookup(
            "reviews",
            "_id",
            "courseId",
            "reviews"
        )
        pipeline += Aggregates.addFields(
            Field(
                "sections",
                BsonDocument("\$map", BsonDocument().apply {
                    put("input", BsonString("\$sections"))
                    put("as", BsonString("section"))
                    put("in", BsonDocument().apply {
                        put("id", BsonString("\$\$section.id"))
                        put("title", BsonString("\$\$section.title"))
                        put(
                            "lectures",
                            BsonDocument("\$map", BsonDocument().apply {
                                put("input", BsonString("\$\$section.lectures"))
                                put("as", BsonString("lecture"))
                                put("in", BsonDocument().apply {
                                    put("id", BsonString("\$\$lecture.id"))
                                    put("name", BsonString("\$\$lecture.name"))
                                    put("duration", BsonString("\$\$lecture.duration"))
                                })
                            })
                        )

                    })
                })
            )

        )
        pipeline += Aggregates.addFields(
            Field("rating", BsonDocument("\$avg", BsonArray().apply {
                add(BsonString("\$reviews.rating"))
            })),
            Field(
                "nbRating",
                BsonDocument(
                    "\$size",
                    BsonString("\$reviews")
                )

            ),
            Field("nbStudents", BsonDocument("\$size", BsonString("\$enrolledStudents"))),
        )

        pipeline += Aggregates.addFields(
            Field("id", Document("\$toString" , BsonString("\$_id"))),
            Field("instructorId", Document("\$toString" ,BsonString("\$instructorId")))
        )

        pipeline += Aggregates.project(
            Projections.exclude(
                "reviews",
                "enrolledStudents"
            )
        )
        if (!fields.isNullOrEmpty()) {
            val projectionFields = Document(fields)
            pipeline += Aggregates.project(projectionFields)
        }



        coursesCollection.aggregate(pipeline)
            .map {
                if (it.containsKey("courseImage")) {
                    val imageBinary = it.get("courseImage", Binary::class.java).data
                    it["courseImage"] = Base64.getEncoder().encodeToString(imageBinary)
                }
                it.toJson()
            }
            .firstOrNull()
    }

    suspend fun courses(
        filters: Map<String, Any> = mapOf(),
        fields: Map<String, Int>? = null,
        sortBy: Map<String, Int> = mapOf(),
        page: Int = 1,
        limit: Int? = Int.MAX_VALUE
    ): List<String> =
        withContext(Dispatchers.IO) {

            val pipeline = mutableListOf<Bson>()
            pipeline += Aggregates.lookup(
                "reviews",
                "_id",
                "courseId",
                "reviews"
            )

            pipeline += Aggregates.lookup(
                "categories", // Foreign collection
                "category",   // Local field (title in courses)
                "title",      // Foreign field (title in categories)
                "categoryDetails" // Output field
            )
            pipeline += Aggregates.unwind(
                "\$categoryDetails",
                UnwindOptions().preserveNullAndEmptyArrays(true)
            )
            pipeline += Aggregates.addFields(
                Field(
                    "categoryDetails.subCategory",
                    Document(
                        "\$cond", listOf(
                            Document(
                                "\$gte", listOf(
                                    Document(
                                        "\$indexOfArray", listOf(
                                            "\$categoryDetails.subCategories.title", "\$subCategory"
                                        )
                                    ),
                                    0
                                )
                            ),
                            Document(
                                "\$arrayElemAt", listOf(
                                    "\$categoryDetails.subCategories",
                                    Document(
                                        "\$indexOfArray", listOf(
                                            "\$categoryDetails.subCategories.title", "\$subCategory"
                                        )
                                    )
                                )
                            ),
                            null
                        )
                    )
                )
            )

            pipeline += Aggregates.project(
                Projections.fields(
                    Projections.computed(
                        "id", Document("\$toString", "\$_id")
                    ),
                    Projections.computed(
                        "instructorId", Document("\$toString", "\$instructorId")
                    ),
                    Projections.computed(
                        "categoryDetails.id", Document("\$toString", "\$categoryDetails._id")
                    ),
                    Projections.include(
                        "title",
                        "subTitle",
                        /*"category",
                        "subCategory",*/
                        "price",
                        "discount",
                        "description",
                        "topic",
                        "level",
                        "courseImage",
                        "duration",
                        "language",
                        "createdAt",
                        "courseTrailer",
                        "courseGoals",
                        "requirements",
                        "targetAudience",
                        "categoryDetails.title",
                        "categoryDetails.icon",
                        "categoryDetails.color",
                        "categoryDetails.subCategory.title",
                    ),
                    Projections.computed(
                        "nbRating",
                        BsonDocument(
                            "\$size",
                            BsonString("\$reviews")
                        )
                    ),
                    Projections.computed(
                        "averageRating", Document(
                            "\$ifNull", listOf(
                                Document("\$avg", "\$reviews.rating"), 0
                            )
                        )
                    ),
                    Projections.computed(
                        "nbEnrolledStudents",
                        BsonDocument(
                            "\$size", BsonString("\$enrolledStudents")
                        )
                    )
                )

            )

            if (filters.isNotEmpty()) {
                pipeline += Aggregates.match(Document(filters))
            }

            if (!fields.isNullOrEmpty()) {
                val projectionFields = Document(fields)
                pipeline += Aggregates.project(projectionFields)
            }


            val sortMap = sortBy.toMutableMap()
            sortMap["_id"] = 1
            pipeline += Aggregates.sort(Document(sortMap))
            limit?.let {
                pipeline += Aggregates.skip((page - 1) * limit)
                pipeline += Aggregates.limit(limit)
            }

            coursesCollection
                .aggregate(pipeline)
                .map { bsonDocument ->

                    if ((fields != null && fields.containsKey("courseImage")) || fields == null) {
                        val courseImage = bsonDocument.get("courseImage") as? Binary
                        val base64Image = courseImage?.let { Base64.getEncoder().encodeToString(it.data) }
                        val updatedDocument = bsonDocument.toMutableMap().apply {
                            this["courseImage"] = base64Image
                        }
                        return@map Document(updatedDocument).toJson()
                    }
                    Document(bsonDocument).toJson()
                }
                .toList()

        }

    suspend fun enrollInCourse(courseId: String, studentId: String) = withContext(Dispatchers.IO) {
        val courseUpdates = Updates.combine(
            Updates.addToSet("enrolledStudents", ObjectId(studentId))
        )
        println("sssttt1:"+studentId)
        println("sssttt2:"+courseId)

        val updateCourseCollection =
            coursesCollection.updateMany(Filters.eq(ObjectId(courseId)), courseUpdates).wasAcknowledged()

        val instructorId =coursesCollection.find(Filters.eq("_id",ObjectId(courseId)))
            .projection(Projections.fields(Projections.computed("instructorId",BsonDocument("\$toString",BsonString("\$instructorId"))),Projections.excludeId())).first()
            ?.toString()?.removePrefix("Document{{instructorId=")?.removeSuffix("}}")
        println("aaaaaaa: $instructorId")

        val studentUpdates =Updates.combine(
            Updates.addToSet("enrolledCourses", ObjectId(courseId)),
            Updates.addToSet("instructors",ObjectId(instructorId))
        )
        val updateStudentCollection =studentsCollection.updateOne(Filters.eq(ObjectId(studentId)), studentUpdates).wasAcknowledged()
        updateCourseCollection && updateStudentCollection

        /*
        try {
            UpdateOptions().upsert(true)
            val courseUpdate = coursesCollection.updateOne(
                Filters.eq("_id", ObjectId(courseId)),
                Updates.addToSet("enrolledStudents", ObjectId(studentId))
            ).wasAcknowledged()

            if (courseUpdate) {
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            println("Error updating wishlist: ${e.message}")
        }
        return@withContext false


          */

        /*
         //val userUpdates = Updates.
         //val updateUserCollection = coursesCollection.updateOne(Filters.eq(ObjectId(courseId)), update).wasAcknowledged()
         */
        //courseUpdate  && studentUpdate

    }
    suspend fun getInstructorId(courseId: String)=withContext(Dispatchers.IO) {
        coursesCollection.find(Filters.eq("_id",ObjectId(courseId)))
            .projection(Projections.fields(Projections.computed("instructorId",BsonDocument("\$toString",BsonString("\$instructorId"))),Projections.excludeId())).first()
            ?.toString()?.removePrefix("Document{{instructorId=")?.removeSuffix("}}")
    }

    /*suspend fun createCourse(course: Course, courseImage: Binary) = withContext(Dispatchers.IO) {
        coursesCollection.insertOne(
            course.toDocument().append("courseImage", courseImage)
        ).insertedId?.asObjectId()?.value.toString()

    }*/

    suspend fun addSection(courseId: String, section: Section) = withContext(Dispatchers.IO) {
        val update = Updates.addToSet("sections", section.toDocument())
        val co = coursesCollection.updateOne(Filters.eq(ObjectId(courseId)), update).wasAcknowledged()
        if (co) section.id["\$oid"]
        else null
    }

    suspend fun deleteSection(courseId: String, sectionId: String) = withContext(Dispatchers.IO) {
        val updateResult = coursesCollection.updateOne(
            Filters.eq("_id", ObjectId(courseId)),
            Updates.pull(
                "sections",
                Filters.eq("id", ObjectId(sectionId))
            )
        )
        updateResult.wasAcknowledged()
    }

    suspend fun addLecture(lecture: Lecture, courseId: String, sectionId: String) = withContext(Dispatchers.IO) {

        val updateResult = coursesCollection.updateOne(
            Filters.and(
                Filters.eq("_id", ObjectId(courseId)),
                Filters.eq("sections.id", ObjectId(sectionId))
            ),
            Updates.push("sections.$[sectionFilter].lectures", lecture.toDocument()),
            UpdateOptions().arrayFilters(
                listOf(Filters.eq("sectionFilter.id", ObjectId(sectionId)))
            )
        ).wasAcknowledged()
        if (updateResult) lecture.id["\$oid"]
        else null
    }

    suspend fun deleteLecture(lectureId: String, courseId: String, sectionId: String) = withContext(Dispatchers.IO) {
        val updateResult = coursesCollection.updateOne(
            Filters.eq("_id", ObjectId(courseId)),
            Updates.pull(
                "sections.$[sectionFilter].lectures",
                Filters.eq("id", ObjectId(lectureId))
            ),
            UpdateOptions().arrayFilters(
                listOf(Filters.eq("sectionFilter.id", ObjectId(sectionId)))
            )
        )
        updateResult.wasAcknowledged()
    }

    suspend fun publicCourse(instructorId: String) = withContext(Dispatchers.IO) {
        val courseDoc = instructorCollection.find(Filters.eq("_id", ObjectId(instructorId)))
            .projection(Projections.include("tempCourse"))
            .first()

        courseDoc?.get("tempCourse")?.let {
            val inserted = coursesCollection.insertOne(it as Document).wasAcknowledged()
            if (inserted) instructorCollection.updateOne(
                Filters.eq(ObjectId(instructorId)),
                Updates.set("tempCourse", null)
            )
            return@withContext inserted
        }
        return@withContext false

    }

    suspend fun calCourseDuration(fileService: FileRepoImpl) = withContext(Dispatchers.IO) {
        val course = coursesCollection.find().firstOrNull()

        if (course == null) {
            println("Course not found!")
            return@withContext
        }

        val sections = course["sections"] as List<Document>
        var totalDuration = 0.0

        sections.forEach { section ->
            val lectures = section["lectures"] as List<Document>
            lectures.forEach { lecture ->

                val duration = fileService.getVideoDuration(lecture.getObjectId("video"))
                lecture["duration"] = duration
                totalDuration += duration
            }
        }

        // Update the course document with the updated sections and total duration
        val updatedCourse = Document(course)
        updatedCourse["sections"] = sections
        updatedCourse["duration"] = totalDuration

        coursesCollection.replaceOne(Document("_id", course.getObjectId("_id")), updatedCourse)
    }

    suspend fun updateCourseImage(courseId: String, image: Binary) = withContext(Dispatchers.IO) {
        coursesCollection.updateOne(
            Filters.eq(ObjectId(courseId)),
            Updates.set("courseImage", image),
            UpdateOptions().upsert(true)
        )
    }

   /* suspend fun coursesByInstructor(instructorId: String) = withContext(Dispatchers.IO) {
        coursesCollection.find(Filters.eq("instructorId" , ObjectId(instructorId))).map{
            val courseImage = it.get("courseImage") as? Binary
            val base64Image = courseImage?.let { Base64.getEncoder().encodeToString(it.data) }
            val updatedDocument = it.toMutableMap().apply {
                this["courseImage"] = base64Image
            }
            Document(updatedDocument).toJson()
        }.toList()
    }*/
}