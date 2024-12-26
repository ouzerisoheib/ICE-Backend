package com.kodea.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.bson.Document
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.Locale


@Serializable
data class Student(
    val firstName: String,
    val lastName: String,
    val userName: String,
    val image: String? = null,
    val email: String,
    val password: String,
    val title : String? = null,
    val roles : Array<Role> = arrayOf(Role.Student),
    val createdAt : Long,
    val enrolledCourses : Array<Map<String , String>> = arrayOf(),
    val instructors : Array<Map<String , String>> = arrayOf(),
    val wishlist : Array<Map<String , String>> = arrayOf(),
    val cart : Array<Map<String , String>> = arrayOf(),

) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Student = json.decodeFromString(document.toJson())
    }
}

@Serializable
data class StudentDTO(
    val firstName: String,
    val lastName: String,
    val userName: String,
    val image: String? = null,
    val email: String,
    val password: String,
    val title : String? = null,
)

fun StudentDTO.toStudent(): Student {
    return Student(
        firstName = firstName,
        lastName = lastName,
        userName = userName,
        image = image,
        email = email,
        password = password,
        title = title,
        roles = arrayOf(Role.Student),
        createdAt =  Clock.System.now().toEpochMilliseconds(),
        enrolledCourses =  arrayOf(),
        instructors = arrayOf(),
        cart =  arrayOf(),
        wishlist = arrayOf(),
    )
}
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        // Convert Instant to ISO-8601 string
        val formatted = value.toJavaInstant().toString() // ISO-8601 format
        encoder.encodeString(formatted)
    }

    override fun deserialize(decoder: Decoder): Instant {
        // Convert ISO-8601 string back to Instant
        val formatted = decoder.decodeString()
        val javaInstant = java.time.Instant.parse(formatted)
        return Instant.fromEpochMilliseconds(javaInstant.toEpochMilli())
    }
}