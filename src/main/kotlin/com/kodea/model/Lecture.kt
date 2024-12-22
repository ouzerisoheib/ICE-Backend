package com.kodea.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class Lecture(
    val id : Map<String , String> = mapOf("\$oid" to ObjectId().toString()),
    val name : String,
    val description : String,
    val caption : String,
    val note : String,
    val video : Map<String , String>,
    val files : Array<Map<String , String>>,
    val duration : Double
){
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Lecture = json.decodeFromString(document.toJson())
    }
}
@Serializable
data class LectureDTO(
    val name : String,
    val description : String,
    val caption : String,
    val note : String,
)
fun LectureDTO.toLecture(videoId : String , filesIds : Array<Map<String, String>> = arrayOf() , duration: Double): Lecture {
    return Lecture(
        id = mapOf("\$oid" to ObjectId().toString()),
        name = this.name,
        description = this.description,
        caption = this.caption,
        note = this.note,
        video = mapOf("\$oid" to videoId),
        files = filesIds,
        duration = duration
    )
}
