package com.kodea.data

import com.kodea.model.*
import com.kodea.utlis.Response
import com.kodea.utlis.generateToken
import com.kodea.utlis.hashedPassword
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
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


class StudentRepoImpl(private val database: MongoDatabase) {
    private var collection: MongoCollection<Document>
    private var coursesCollection: MongoCollection<Document>
    //private var instructorsCollection: MongoCollection<Document>
    //val gridFSBucket: GridFSBucket = GridFSBuckets.insert(database)

    init {
        database.createCollection("users")
        collection = database.getCollection("users")
        coursesCollection = database.getCollection("courses")
        //instructorsCollection = database.getCollection("instructors")
    }

    suspend fun login(email: String, password: String) =
        withContext(Dispatchers.IO) {
            //val hashedPassword = hashedPassword(password)
            val user = collection.find(Filters.eq("email", email)).first()
            if (user != null && BCrypt.checkpw(password, user.getString("password"))) {
                val token = generateToken(user.getObjectId("_id").toHexString(), Role.Student)
                Response.Success(token)
            } else {
                Response.Failure("Invalid Email or Password")
            }
        }

    // Insert one student
    suspend fun insert(student: Student): String = withContext(Dispatchers.IO) {
        val doc = student.toDocument()
        collection.insertOne(doc)
        doc["_id"].toString()
    }

    // Read a student
    suspend fun read(id: String): String? = withContext(Dispatchers.IO) {
        val pipeline = mutableListOf<Bson>()
        pipeline += Aggregates.match(Filters.eq(ObjectId(id)))
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
        collection.find(Filters.eq("email", email)).projection(Projections.exclude("password")).first()?.let(Student::fromDocument)
    }

    suspend fun update(id: String, student: Student): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(Filters.eq("_id", ObjectId(id)), student.toDocument())
    }

    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }


    suspend fun getInstructors() = withContext(Dispatchers.IO) {
        try {

            val doc = coursesCollection.find()
                .map(Doc.Companion::fromDocument).toList()
            if (doc.isNotEmpty()) {
                for (i in doc[0].instructors) {
                    val instructor = i.toDocument()
                    collection.insertOne(instructor)
                }
            }
            "added succefully"
        } catch (e: Exception) {
            "error : ${e.message}"
        }

    }


}

//fun jsonToMap(json: String): Map<String, Any> = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject.toMap()
fun Document.toMap(): Map<String, Any?> {
    val jsonString = this.toJson() // Correct way to convert BSON to JSON string
    return kotlinx.serialization.json.Json.decodeFromString(
        kotlinx.serialization.json.JsonObject.serializer(),
        jsonString
    ).toMap()
}

@Serializable
data class Doc(
    val courses: List<Course>,
    val categories: List<Category>,
    val instructors: List<Instructor>
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Doc = json.decodeFromString(document.toJson())
    }
}

