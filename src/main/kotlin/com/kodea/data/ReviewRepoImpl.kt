package com.kodea.data

import com.kodea.model.Review
import com.kodea.model.ReviewDTO
import com.kodea.model.Student
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId

class ReviewRepoImpl(database: MongoDatabase) {
    private var collection: MongoCollection<Document>

    init {
        database.createCollection("reviews")
        collection = database.getCollection("reviews")
    }

    suspend fun addReview(review: Review) = withContext(Dispatchers.IO) {
        collection.insertOne(review.toDocument()).wasAcknowledged()
    }

    suspend fun getReview(id: String) = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(
            Filters.eq("_id", ObjectId(id)),
        )
        pipeline += Aggregates.lookup(
            "users",
            "studentId",
            "_id",
            "student"
        )
        pipeline += Aggregates.unwind("\$student")

        pipeline += Aggregates.project(
            Projections.fields(


                Projections.include(
                    "courseId",
                    "studentId",
                    "student.firstName",
                    "student.lastName",
                    "student.image",
                    "comment",
                    "rating",
                    "createdAt"
                ),
                Projections.computed("_id", BsonDocument("\$toString", BsonString("\$_id"))),
                Projections.computed("courseId", BsonDocument("\$toString", BsonString("\$courseId"))),
                Projections.computed("studentId", BsonDocument("\$toString", BsonString("\$studentId")))
            )
        )
        collection.aggregate(pipeline).first()?.toJson()
    }

    suspend fun getReviews(of: String = "courseId", id: String) = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(
            if (of == "courseId") Filters.eq(of, ObjectId(id))
            else Filters.eq("studentId", ObjectId(id))
        )
        pipeline += Aggregates.lookup(
            "users",
            "studentId",
            "_id",
            "student"
        )
        pipeline += Aggregates.unwind("\$student")
        pipeline += Aggregates.project(
            Projections.fields(


                Projections.include(
                    "courseId",
                    "studentId",
                    "student.firstName",
                    "student.lastName",
                    "student.image",
                    "comment",
                    "rating",
                    "createdAt"
                ),
                Projections.computed("_id", BsonDocument("\$toString", BsonString("\$_id"))),
                Projections.computed("courseId", BsonDocument("\$toString", BsonString("\$courseId"))),
                Projections.computed("studentId", BsonDocument("\$toString", BsonString("\$studentId")))
            )
        )
        collection.aggregate(pipeline).first()?.toJson()
    }
}