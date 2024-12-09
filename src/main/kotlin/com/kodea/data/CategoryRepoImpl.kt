package com.kodea.data

import com.kodea.model.Category
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.conversions.Bson

class CategoryRepoImpl(database : MongoDatabase) {
    private var collection: MongoCollection<Document>

    init {
        database.createCollection("categories")
        collection = database.getCollection("categories")
    }

    suspend fun addCategory(category: Category) = withContext(Dispatchers.IO){

    }
    suspend fun getCategories() = withContext(Dispatchers.IO){
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.lookup(
            "courses",
            "title",
            "category",
            "courses"
        )
        pipeline += Aggregates.addFields(
            Field(
                "nbCourses",
                BsonDocument("\$size" , BsonString("\$courses"))
            )
        )

        collection.aggregate(pipeline).map { it.toJson() }.toList()
    }
}