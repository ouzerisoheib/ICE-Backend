package com.kodea.data

import com.kodea.model.Instructor
import com.kodea.model.Role
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId

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
                Filters.`in`("roles" , Role.Instructor)
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
                BsonDocument("\$map" , BsonDocument().apply {
                    put("input" , BsonString("\$publishedCourses"))
                    put("as" , BsonString("course"))
                    put("in" , BsonDocument().apply {
                        put("id" , BsonString("\$\$course._id"))
                        put("nbStudents" , BsonDocument("\$size" , BsonString("\$\$course.enrolledStudents")))
                    })
                })
            )
        )
        pipeline += Aggregates.addFields(
            Field(
                "nbEnrolledStudents",
                BsonDocument("\$sum" , BsonString("\$publishedCourses.nbStudents"))
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

        collection.aggregate(pipeline).first()?.toJson()

    }

    suspend fun upadateInstructor(id: String) = withContext(Dispatchers.IO) {

    }

}