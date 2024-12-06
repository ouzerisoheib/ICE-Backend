package com.kodea.model


import com.kodea.utlis.ObjectIdSerializer
import kotlinx.datetime.Clock
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.Binary
import org.bson.types.ObjectId
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Locale.LanguageRange

/**
   * @Param duration is the duration of course in seconds
*/
@Serializable
data class Course(
    @BsonId
    val id : String? = null,

    val instructorId: Map<String , String>?,
    val title : String,
    val subTitle : String,
    val category : String,
    val subCategory : String,
    val price : Int,
    val discount : Int,
    val description : String,
    val topic : String,
    val level : Level,
    val duration : Double,
    val thumbnail : String? = null,
    val videoTrailer : String? = null,
    val courseGoals : List<String>,
    val requirements : List<String>,
    val targetAudience : List<String>,
    val language : String = Locale.UK.language,
    val sections : List<Section>,
    @Contextual
    val createdAt : kotlinx.datetime.Instant,
    val enrolledStudents : List<String>
){
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Course = json.decodeFromString(document.toJson())
    }
}

@Serializable
data class CourseDTO(
    @BsonId
    val instructorId : String? = ObjectId().toString(),
    val title : String,
    val subTitle : String,
    val category : String,
    val subCategory : String,
    val price : Int,
    val discount : Int,
    val description : String,
    val topic : String,
    val level : Level,
    val duration : Double,
    val thumbnail : String? = null,
    val videoTrailer : String? = null,
    val courseGoals : List<String>,
    val requirements : List<String>,
    val targetAudience : List<String>,
    val language : String = Locale.UK.language,
    val sections : List<Section>,
)

fun CourseDTO.toCourse() : Course = Course(
    id = null,
    instructorId = mapOf("\$oid" to this.instructorId!!),
    title = this.title,
    subTitle = this.subTitle,
    category = this.category,
    subCategory = this.subCategory,
    price = this.price,
    discount = this.discount,
    description = this.description,
    topic = this.topic,
    level = this.level,
    duration = this.duration,
    thumbnail = this.thumbnail,
    videoTrailer = this.videoTrailer,
    courseGoals = this.courseGoals,
    requirements = this.requirements,
    targetAudience = this.targetAudience,
    language = this.language,
    sections = this.sections,
    createdAt = Clock.System.now(),
    enrolledStudents = emptyList()
)

/*
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli()) // Serialize as milliseconds.
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeLong()) // Deserialize from milliseconds.
    }
}*/
