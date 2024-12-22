package com.kodea.data

import com.kodea.model.*
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.Binary
import org.bson.types.ObjectId
import java.util.*

class InstructorRepoImpl(private val database: MongoDatabase) {
    private var collection: MongoCollection<Document>
    private var coursesCollection: MongoCollection<Document>


    init {
        database.createCollection("users")
        collection = database.getCollection("users")
        coursesCollection = database.getCollection("courses")

    }

    suspend fun beInstructor(id: String) = withContext(Dispatchers.IO) {
        val update = Updates.combine(
            Updates.addToSet("roles", Role.Instructor)
        )
        collection.updateOne(Filters.eq("_id", ObjectId(id)), update)
    }

    suspend fun getInstructor(id: String) = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(
            Filters.and(
                Filters.eq(ObjectId(id)),
                Filters.`in`("roles", Role.Instructor)
            )
        )
        pipeline += Aggregates.lookup(
            "courses",
            "_id",
            "instructorId",
            "publishedCourses"
        )
        pipeline += Aggregates.addFields(
            Field(
                "publishedCourses",
                BsonDocument("\$map", BsonDocument().apply {
                    put("input", BsonString("\$publishedCourses"))
                    put("as", BsonString("course"))
                    put("in", BsonDocument().apply {
                        put("id", BsonString("\$\$course._id"))
                        put("nbStudents", BsonDocument("\$size", BsonString("\$\$course.enrolledStudents")))
                    })
                })
            )
        )
        pipeline += Aggregates.addFields(
            Field(
                "nbEnrolledStudents",
                BsonDocument("\$sum", BsonString("\$publishedCourses.nbStudents"))
            )
        )
        pipeline += Aggregates.lookup(
            "reviews",
            "publishedCourses.id",
            "courseId",
            "reviews"
        )
        pipeline += Aggregates.addFields(
            Field(
                "rating",
                BsonDocument(
                    "\$avg",
                    BsonString("\$reviews.rating")
                )
            )
        )

        collection.aggregate(pipeline).map {
            if (it.containsKey("image")) {
                val imageBinary = it.get("image", Binary::class.java).data
                it["image"] = Base64.getEncoder().encodeToString(imageBinary)
            }
            it.toJson()
        }.firstOrNull()

    }

    suspend fun getInstructors() = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(
            Filters.and(
                Filters.`in`("roles", Role.Instructor)
            )
        )
        pipeline += Aggregates.lookup(
            "courses",
            "_id",
            "instructorId",
            "publishedCourses"
        )
        pipeline += Aggregates.addFields(
            Field(
                "publishedCourses",
                BsonDocument("\$map", BsonDocument().apply {
                    put("input", BsonString("\$publishedCourses"))
                    put("as", BsonString("course"))
                    put("in", BsonDocument().apply {
                        put("id", BsonString("\$\$course._id"))
                        put("nbStudents", BsonDocument("\$size", BsonString("\$\$course.enrolledStudents")))
                    })
                })
            )
        )
        pipeline += Aggregates.addFields(
            Field(
                "nbEnrolledStudents",
                BsonDocument("\$sum", BsonString("\$publishedCourses.nbStudents"))
            )
        )
        pipeline += Aggregates.lookup(
            "reviews",
            "publishedCourses.id",
            "courseId",
            "reviews"
        )
        pipeline += Aggregates.addFields(
            Field(
                "rating",
                BsonDocument(
                    "\$avg",
                    BsonString("\$reviews.rating")
                )
            )
        )

        collection.aggregate(pipeline).map {
            if (it.containsKey("image")) {
                val imageBinary = it.get("image", Binary::class.java).data
                it["image"] = Base64.getEncoder().encodeToString(imageBinary)
            }
            it.toJson()
        }.toList()
    }


    suspend fun createCourse(instructorId: String, course: Course, courseImage: Binary) = withContext(Dispatchers.IO) {
        //collection.insertOne(course.toDocument().append("courseImage" , courseImage)).insertedId?.asObjectId()?.value.toString()
        val result = collection.updateOne(
            Filters.eq("_id", ObjectId(instructorId)),
            Updates.set("tempCourse", course.toDocument().append("courseImage", courseImage)),
            UpdateOptions().upsert(true)  // Ensures that the tempCourse field is created if it does not exist
        )
        result

    }

    suspend fun getCourse(instructorId: String) = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(Filters.eq(ObjectId(instructorId)))
        pipeline += Aggregates.project(
            Projections.fields(
                Projections.include("tempCourse"),
                Projections.excludeId()
            )

        )
        collection.aggregate(pipeline).map {
            if (it.containsKey("tempCourse.courseImage")) {
                val imageBinary = it.get("tempCourse.courseImage", Binary::class.java).data
                it["tempCourse.courseImage"] = Base64.getEncoder().encodeToString(imageBinary)
            }
            it.toJson()
        }.first()
    }

    suspend fun addSection(instructorId: String, section: Section) = withContext(Dispatchers.IO) {
        val update = Updates.addToSet("tempCourse.sections", section.toDocument())
        val co = collection.updateOne(Filters.eq(ObjectId(instructorId)), update).wasAcknowledged()
        if (co) section.id["\$oid"]
        else ""
    }

    suspend fun deleteSection(instructorId: String, sectionId: String) = withContext(Dispatchers.IO) {
        val updateResult = collection.updateOne(
            Filters.eq("_id", ObjectId(instructorId)),
            Updates.pull(
                "tempCourse.sections",
                Filters.eq("id", ObjectId(sectionId))
            )
        )
        updateResult
    }

    suspend fun addLecture(instructorId: String, lecture: Lecture, sectionId: String) = withContext(Dispatchers.IO) {

        val updateResult = collection.updateOne(
            Filters.and(
                Filters.eq("_id", ObjectId(instructorId)),
                Filters.eq("tempCourse.sections.id", ObjectId(sectionId))
            ),
            Updates.combine(
                Updates.push("tempCourse.sections.$[sectionFilter].lectures", lecture.toDocument()),
                Updates.inc("tempCourse.duration", lecture.duration)
            ),
            UpdateOptions().arrayFilters(
                listOf(Filters.eq("sectionFilter.id", ObjectId(sectionId)))
            )
        )

        updateResult
    }

    suspend fun deleteLecture(instructorId: String, lectureId: String, sectionId: String) =
        withContext(Dispatchers.IO) {
            val updateResult = collection.updateOne(
                Filters.eq("_id", ObjectId(instructorId)),
                Updates.pull(
                    "tempCourse.sections.$[sectionFilter].lectures",
                    Filters.eq("id", ObjectId(lectureId))
                ),
                UpdateOptions().arrayFilters(
                    listOf(Filters.eq("sectionFilter.id", ObjectId(sectionId)))
                )
            )
            updateResult
        }

    suspend fun updateInstructorImage(instructorId: String, image: Binary) = withContext(Dispatchers.IO) {
        collection.updateOne(
            Filters.eq(ObjectId(instructorId)),
            Updates.set("image", image),
            UpdateOptions().upsert(true)
        )
    }


}