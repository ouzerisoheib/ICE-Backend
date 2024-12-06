package com.kodea.data

import com.kodea.model.Instructor
import com.kodea.model.Role
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun beInstructor(id : String) = withContext(Dispatchers.IO){
        val update = Updates.combine(
            Updates.addToSet("roles" , Role.Instructor)
        )
        collection.updateOne(Filters.eq("_id", ObjectId(id)), update)
    }

    suspend fun getInstructor(id: String) = withContext(Dispatchers.IO){
        collection.find(Filters.and(Filters.eq(ObjectId(id)) , Filters.`in`("roles" , Role.Instructor))).first()?.toJson()

    }
    suspend fun getInstructors() = withContext(Dispatchers.IO){
        val pipeline = mutableListOf<Bson>()


    }
}