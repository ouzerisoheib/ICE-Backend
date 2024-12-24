package com.kodea.data

import com.kodea.model.*
import com.kodea.utlis.Response
import com.kodea.utlis.generateToken
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt


class StudentRepoImpl( database: MongoDatabase) {
    private var collection: MongoCollection<Document>
    private var coursesCollection: MongoCollection<Document>
    //private var instructorsCollection: MongoCollection<Document>
    //val gridFSBucket: GridFSBucket = GridFSBuckets.register(database)

    init {
        database.createCollection("users")
        collection = database.getCollection("users")
        coursesCollection = database.getCollection("courses")
        //instructorsCollection = database.getCollection("instructors")
    }

    suspend fun login(email: String, password: String) =
        withContext(Dispatchers.IO) {
            //val hashedPassword = hashedPassword(password)
            val user = collection.find(eq("email", email)).first()
            if (user != null && BCrypt.checkpw(password, user.getString("password"))) {
                val token = generateToken(user.getObjectId("_id").toHexString(), Role.Student)
                Response.Success(token)
            } else {
                Response.Failure("Invalid Email or Password")
            }
        }

    // Insert one student
    suspend fun register(student: Student): String = withContext(Dispatchers.IO) {
        val doc = student.toDocument()
        collection.insertOne(doc)
        doc["_id"].toString()
    }

    // Read a student
    suspend fun read(id: String): String? = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(eq(ObjectId(id)))
        pipeline += Aggregates.lookup(
            "courses",
            "_id",
            "enrolledStudents._id",
            "enrolledCourses"
        )

        pipeline += Aggregates.addFields(
            Field(
                "nbEnrolledCourses",
                BsonDocument("\$size" , BsonString("\$enrolledCourses"))
            )
        )
        pipeline += Aggregates.lookup(
            "users",
            "enrolledCourses.instructorId",
            "_id",
            "instructors"
        )
        pipeline += Aggregates.addFields(
            Field(
                "enrolledCourses",
                BsonDocument(
                    "\$map" , BsonDocument().apply {
                        put("input" , BsonString("\$enrolledCourses"))
                        put("as" , BsonString("course"))
                        put("in" , BsonDocument().apply {
                            put("id" , BsonDocument("\$toString",BsonString("\$\$course._id")))
                        })
                    }
                )
            )
        )
        pipeline += Aggregates.addFields(
            Field(
                "instructors",
                BsonDocument(
                    "\$map" , BsonDocument().apply {
                        put("input" , BsonString("\$instructors"))
                        put("as" , BsonString("instructor"))
                        put("in" , BsonDocument().apply {
                            put("id" , BsonDocument("\$toString",BsonString("\$\$instructor._id")))
                            //put("firstName" , BsonString("\$\$instructor.firstName"))
                            //put("lastName" , BsonString("\$\$instructor.lastName"))
                            //put("image" , BsonString("\$\$instructor.image"))
                        })
                    }
                )
            )
        )
        //pipeline += Aggregates.unwind("\$enrolled_courses")
        /*pipeline += Aggregates.project(Projections.fields(
            Projections.include(), // Includes all fields from the student document
            //Projections.include("enrolledCourses") // Include the enrolled_courses array
        ))*/
        collection.aggregate(pipeline).first()?.toJson()
    }

    suspend fun readAll(): List<Student> = withContext(Dispatchers.IO) {
        collection.find().projection(Projections.exclude("password")).map(Student::fromDocument).toList()
    }


    suspend fun readByEmail(email: String) = withContext(Dispatchers.IO) {
        collection.find(eq("email", email)).projection(Projections.exclude("password")).first()?.let(Student::fromDocument)
    }

    suspend fun update(id: String, student: Student): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(eq("_id", ObjectId(id)), student.toDocument())
    }

    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(eq("_id", ObjectId(id)))
    }

    suspend fun getWishlist(studentId:String):String? =withContext(Dispatchers.IO) {
        collection.find(eq("_id",ObjectId(studentId))).projection(Projections.include("wishlist")).first()?.toJson()
    }
    suspend fun addToWishlist(studentId:String ,courseId:String):Boolean =withContext(Dispatchers.IO) {
        try {
            UpdateOptions().upsert(true)
            val result = collection.updateOne(
                Filters.eq("_id", ObjectId(studentId)),
                Updates.addToSet("wishlist", ObjectId(courseId))
            ).wasAcknowledged()

            if (result) {
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            println("Error updating wishlist: ${e.message}")
        }
        return@withContext false
        //val studentUpdates = Updates.combine( Updates.addToSet("wishlist", ObjectId(courseId)) )
        //val updateStudentCollection =collection.updateOne(eq(ObjectId(studendId)), studentUpdates).wasAcknowledged()
        //updateStudentCollection

    }
    suspend fun removeFromWishlist(studentId:String ,courseId:String):Boolean=withContext(Dispatchers.IO) {
        //val studentUpdates = Updates.combine(Updates.pullByFilter(eq("wishlist", ObjectId(courseId)))
            try {

                val result = collection.updateOne(
                    Filters.eq("_id",ObjectId(studentId)),
                    Updates.pullByFilter(Filters.eq("wishlist",ObjectId(courseId))),
                ).wasAcknowledged()
                if (result) {
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                println("Error removing from wishlist: ${e.message}")
            }
            return@withContext false
        }

    suspend fun getCart(studentId:String):String? =withContext(Dispatchers.IO) {
        collection.find(eq("_id",ObjectId(studentId))).projection(Projections.include("cart")).first()?.toJson()
    }
    suspend fun addToCart(studentId:String ,courseId:String):Boolean =withContext(Dispatchers.IO) {
        try {
            UpdateOptions().upsert(true)
            val result = collection.updateOne(
                Filters.eq("_id", ObjectId(studentId)),
                Updates.addToSet("cart", ObjectId(courseId))
            ).wasAcknowledged()

            if (result) {
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            println("Error updating cart: ${e.message}")
        }
        return@withContext false

    }
    suspend fun removeFromCart(studentId:String ,courseId:String):Boolean=withContext(Dispatchers.IO) {
        try {

            val result = collection.updateOne(
                Filters.eq("_id",ObjectId(studentId)),
                Updates.pullByFilter(Filters.eq("cart",ObjectId(courseId))),
            ).wasAcknowledged()
            if (result) {
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            println("Error removing from cart: ${e.message}")
        }
        return@withContext false
    }

/*
    suspend fun addToCart(studendId:String ,courseId:String)=withContext(Dispatchers.IO) {
        val studentUpdates = Updates.combine(
            Updates.addToSet("cart", ObjectId(courseId))
        )
        val updateStudentCollection =collection.updateOne(eq(ObjectId(studendId)), studentUpdates).wasAcknowledged()
        updateStudentCollection
    }
    suspend fun removeFromCart(studendId:String ,courseId:String)=withContext(Dispatchers.IO) {
        val studentUpdates = Updates.combine(
            Updates.pullByFilter(eq("cart", ObjectId(courseId)))
        )
        val updateStudentCollection =collection.updateOne(eq(ObjectId(studendId)), studentUpdates).wasAcknowledged()
        updateStudentCollection
    }

 */

}

//fun jsonToMap(json: String): Map<String, Any> = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject.toMap()
fun Document.toMap(): Map<String, Any?> {
    val jsonString = this.toJson() // Correct way to convert BSON to JSON string
    return kotlinx.serialization.json.Json.decodeFromString(
        kotlinx.serialization.json.JsonObject.serializer(),
        jsonString
    ).toMap()
}


