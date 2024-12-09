package com.kodea.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document

@Serializable
data class Review(
    val courseId : Map<String , String>,
    val studentId : Map<String , String>,
    val rating : Double? = null,
    val comment : String,
    val createdAt : Instant,
){
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Review = json.decodeFromString(document.toJson())
    }
}
@Serializable
data class ReviewDTO(
    val courseId : Map<String , String>,
    val studentId : Map<String , String>,
    val rating : Double? = null,
    val comment : String
)
fun ReviewDTO.toReview() : Review {
    return Review(
        courseId = courseId,
        studentId = studentId,
        rating = rating,
        comment = comment,
        createdAt = Clock.System.now(),
    )
}