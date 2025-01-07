package com.kodea.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class User(
    @BsonId
    val id: String? = null,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val createdAt: String = Clock.System.now().toString()
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromDocument(document: Document): User = json.decodeFromString(document.toJson())
    }
}
@Serializable
data class UserDTO(
    val firstName: String,
    val lastName: String,
    val userName: String,
    val email: String,
    val roles: List<String>
)

fun User.toUserDTO(): UserDTO = UserDTO(
    firstName = firstName,
    lastName = lastName,
    userName = userName,
    email = email,
    roles = roles
)
