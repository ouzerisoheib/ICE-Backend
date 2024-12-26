package com.kodea.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
@Serializable
data class Answer(
    @BsonId
    val id: String? = null,
    val userId: Map<String, String>,
    val userRole: String,
    val description: String,
    val files: List<String> = emptyList(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val votes: Int = 0,
    val parentId: Map<String, String>? = null,  // For replies to answers
    val replies: List<Map<String, String>> = emptyList()
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromDocument(document: Document): Answer = json.decodeFromString(document.toJson())
    }
}

@Serializable
data class AnswerDTO(
    var userRole: String,
    val description: String,
    val files: List<String> = emptyList()
)

fun AnswerDTO.toAnswer(userId: String, parentId: String? = null) = Answer(
    userId = mapOf("\$oid" to userId),
    userRole = userRole,
    description = description,
    files = files,
    parentId = parentId?.let { mapOf("\$oid" to it) }
)