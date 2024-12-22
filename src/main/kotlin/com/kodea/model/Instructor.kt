package com.kodea.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class Instructor(
    val firstName: String,
    val lastName: String,
    val userName: String,
    val image: String? = null,
    val email: String,
    val password: String,
    val title : String? = null,
    val roles : Array<Role> = arrayOf(Role.Instructor),
    val createdAt : Instant = Clock.System.now(),
){
    /*fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Instructor {
            val id = document.getObjectId("_id").toHexString()
            return json.decodeFromString<Instructor>(document.toJson()).copy(id = id.toInt())
        }
    }*/
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Instructor = json.decodeFromString(document.toJson())
    }
}
