package com.kodea.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class Section(

    val id : Map<String , String> = mapOf("\$oid" to ObjectId().toString()),
    val title : String,
    val lectures : Array<Lecture> = arrayOf()
){
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Section = json.decodeFromString(document.toJson())
    }
}
