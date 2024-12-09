package com.kodea.data

import com.kodea.model.Course
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

class CourseRepoImpl(private val database: MongoDatabase) {

    private var coursesCollection: MongoCollection<Document>

    init {
        database.createCollection("courses")
        coursesCollection = database.getCollection("courses")
    }

    suspend fun addCourse(course: Course, courseImage: Binary): ObjectId? {
        return withContext(Dispatchers.IO) {
            val result = coursesCollection.insertOne(course.toDocument().append("courseImage", courseImage))
            result.insertedId?.asObjectId()?.value
        }
    }

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
                "reviews",
                BsonDocument("\$map", BsonDocument().apply {
                    put("input", BsonString("\$reviews"))
                    put("as", BsonString("review"))
                    put("in", BsonDocument().apply {
                        put("_id", BsonString("\$\$review._id"))
                        /*put("studentId", BsonString("\$\$review.studentId"))
                        put("rating", BsonString("\$\$review.rating"))
                        put("comment", BsonString("\$\$review.comment"))
                        put("createdAt", BsonString("\$\$review.createdAt"))
                        put("updatedAt", BsonString("\$\$review.updatedAt"))*/
                    })
                })
            )
        )
        pipeline += Aggregates.addFields(
            Field("rating", BsonDocument("\$avg", BsonArray().apply {
                add(BsonString("\$reviews.rating"))
            })),
            Field("nbStudents", BsonDocument("\$size", BsonString("\$enrolledStudents")))
        )
        if (!fields.isNullOrEmpty()) {
            val projectionFields = Document(fields)
            pipeline += Aggregates.project(projectionFields)
        }
        pipeline += Aggregates.project(
            Projections.exclude(
                "reviews",
                "enrolledStudents"
            )
        )



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
        limit: Int?
    ): List<String> =
        withContext(Dispatchers.IO) {

            val pipeline = mutableListOf<Bson>()

            pipeline += Aggregates.lookup(
                "reviews",
                "_id",
                "courseId",
                "reviews"
            )
            pipeline += Aggregates.addFields(
                Field(
                    "reviews",
                    BsonDocument("\$map", BsonDocument().apply {
                        put("input", BsonString("\$reviews"))
                        put("as", BsonString("review"))
                        put("in", BsonDocument().apply {
                            put("_id", BsonString("\$\$review._id"))
                            /*put("studentId", BsonString("\$\$review.studentId"))
                            put("rating", BsonString("\$\$review.rating"))
                            put("comment", BsonString("\$\$review.comment"))
                            put("createdAt", BsonString("\$\$review.createdAt"))
                            put("updatedAt", BsonString("\$\$review.updatedAt"))*/
                        })
                    })
                )
            )
            pipeline += Aggregates.addFields(
                Field(
                    "nbEnrolledStudents",
                    BsonDocument(
                        "\$size", BsonString("\$enrolledStudents")
                    )
                )
            )
            pipeline += Aggregates.addFields(
                Field(
                    "rating",
                    BsonDocument(
                        "\$ifNull", BsonArray().apply {
                            add(
                                BsonDocument(
                                    "\$avg", BsonArray().apply {
                                        add(BsonString("\$reviews.rating"))
                                    }
                                )
                            )
                            add(BsonDouble(0.0))
                        }
                    )
                ),
                Field(
                    "nbRating",
                    BsonDocument(
                        "\$size",
                        BsonString("\$reviews")
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
                .map { bsonDocument -> bsonDocument.toJson() }
                .toList()

        }

    suspend fun enrollInCourse(courseId: String, studentId: String) = withContext(Dispatchers.IO) {
        val courseUpdates = Updates.combine(
            Updates.addToSet("enrolledStudents", ObjectId(studentId))
        )
        val updateCourseCollection =
            coursesCollection.updateOne(Filters.eq(ObjectId(courseId)), courseUpdates).wasAcknowledged()
        //val userUpdates = Updates.
        //val updateUserCollection = coursesCollection.updateOne(Filters.eq(ObjectId(courseId)), update).wasAcknowledged()
        updateCourseCollection
    }


    suspend fun createCourse(course : Course , courseImage: Binary) = withContext(Dispatchers.IO){
        coursesCollection.insertOne(course.toDocument().append("courseImage" , courseImage)).insertedId?.asObjectId()?.value.toString()

    }
    suspend fun addSection(courseId : String ,section : Section ) = withContext(Dispatchers.IO){
        val update = Updates.addToSet("sections", section.toDocument())
        val co = coursesCollection.updateOne(Filters.eq(ObjectId(courseId)), update).wasAcknowledged()
        if (co) section.id["\$oid"]
        else ""
    }
    suspend fun addLecture(lecture : Lecture) = withContext(Dispatchers.IO){}
}