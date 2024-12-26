import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class Question(
    @BsonId
    val id: String? = null,
    val title: String,
    val description: String,
    val files: List<String> = emptyList(),
    val userId: Map<String, String>,
    val courseId: Map<String, String>,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val answers: List<Map<String, String>> = emptyList(),
    val section: String,
    val lecture: String,
    val views: Int = 0,
    val votes: Int = 0
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromDocument(document: Document): Question = json.decodeFromString(document.toJson())
    }
}

@Serializable
data class QuestionDTO(
    val title: String,
    val description: String,
    val files: List<String> = emptyList(),
    val section: String,
    val lecture: String
)

fun QuestionDTO.toQuestion() = Question(
    title = title,
    description = description,
    files = files,
    userId = mapOf(),
    courseId = mapOf(),
    section = section,
    lecture = lecture
)
