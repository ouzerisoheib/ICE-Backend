package com.kodea.data

import Question
import QuestionDTO
import com.kodea.model.*
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import toQuestion

class ForumRepoImpl(private val database: MongoDatabase) {
    private var questionsCollection: MongoCollection<Document>
    private var coursesCollection: MongoCollection<Document>
    private val answersCollection: MongoCollection<Document>
    private val votesCollection: MongoCollection<Document>
    private val usersCollection: MongoCollection<Document>

    init{
        database.createCollection("courses")
        questionsCollection = database.getCollection("questions")
        coursesCollection = database.getCollection("courses")
        answersCollection = database.getCollection("answers")
        votesCollection = database.getCollection("votes")
        usersCollection = database.getCollection("users")

    }

suspend fun addQuestion(questionDTO: QuestionDTO, courseId: String, userId: String) = withContext(Dispatchers.IO) {
    val question = questionDTO.toQuestion().copy(courseId = mapOf("\$oid" to courseId))

    val questionResult = questionsCollection.insertOne(question.toDocument())
    if (!questionResult.wasAcknowledged()) return@withContext null

    val questionId = questionResult.insertedId?.asObjectId()?.value.toString()

    val courseUpdate = coursesCollection.updateOne(
        Filters.eq("_id", ObjectId(courseId)),
        Updates.push("questions", mapOf("\$oid" to questionId))
    )

    if (courseUpdate.wasAcknowledged()) questionId else null
}
    suspend fun getQuestions(courseId: String): List<Question> = withContext(Dispatchers.IO) {
        try {
            val objectId = ObjectId(courseId)
            println("ObjectId for courseId: $objectId")

            val result = questionsCollection
                .find(Filters.eq("courseId", objectId))
                .toList()

            println("Raw query result: $result")

            val questions = result.map { Question.fromDocument(it) }
            println("Mapped questions: $questions")

            return@withContext questions
        } catch (e: Exception) {
            println("Error fetching questions: ${e.message}")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun addAnswer(answerDTO: AnswerDTO, questionId: String, userId: String): String? = withContext(Dispatchers.IO) {
        val answer = answerDTO.toAnswer(userId)
        val answerResult = answersCollection.insertOne(answer.toDocument())
        if (!answerResult.wasAcknowledged()) return@withContext null

        val answerId = answerResult.insertedId?.asObjectId()?.value.toString()
        val questionUpdate = questionsCollection.updateOne(
            Filters.eq("_id", ObjectId(questionId)),
            Updates.push("answers", mapOf("\$oid" to answerId))
        )

        if (questionUpdate.wasAcknowledged()) answerId else null
    }

    suspend fun getAnswers(questionId: String): List<Answer> = withContext(Dispatchers.IO) {
        val question = questionsCollection
            .find(Filters.eq("_id", ObjectId(questionId)))
            .firstOrNull()
            ?.let { Question.fromDocument(it) }
            ?: return@withContext emptyList()

        val answerIds = question.answers.mapNotNull { it["\$oid"] }
        val answerDocuments = answersCollection
            .find(Filters.`in`("_id", answerIds.map { ObjectId(it) }))
            .toList()

        answerDocuments.map { Answer.fromDocument(it) }
    }

    suspend fun vote(type: String, id: String, userId: String, isUpvote: Boolean): Boolean = withContext(Dispatchers.IO) {
        val collection = when(type) {
            "questions" -> questionsCollection
            "answers" -> answersCollection
            else -> return@withContext false
        }

        val update = Updates.inc("votes", if (isUpvote) 1 else -1)
        collection.updateOne(
            Filters.eq("_id", ObjectId(id)),
            update
        ).wasAcknowledged()
    }

    suspend fun getVoteCount(type: String, id: String): Int = withContext(Dispatchers.IO) {
        val collection = when(type) {
            "questions" -> questionsCollection
            "answers" -> answersCollection
            else -> return@withContext 0
        }

        collection
            .find(Filters.eq("_id", ObjectId(id)))
            .firstOrNull()
            ?.getInteger("votes") ?: 0
    }

    suspend fun incrementViews(id: String): Boolean = withContext(Dispatchers.IO) {
        questionsCollection.updateOne(
            Filters.eq("_id", ObjectId(id)),
            Updates.inc("views", 1)
        ).wasAcknowledged()
    }

    suspend fun getViewCount(id: String): Int = withContext(Dispatchers.IO) {
        questionsCollection
            .find(Filters.eq("_id", ObjectId(id)))
            .firstOrNull()
            ?.getInteger("views") ?: 0
    }
    suspend fun checkEnrollment(courseId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        val course = coursesCollection
            .find(Filters.eq("_id", ObjectId(courseId)))
            .firstOrNull()
            ?: return@withContext false

        val enrolledStudents = course.getList("enrolledStudents", String::class.java) ?: emptyList()
        return@withContext enrolledStudents.contains(userId)
    }
    suspend fun getQuestion(questionId: String): Question? = withContext(Dispatchers.IO) {
        try {
            questionsCollection
                .find(Filters.eq("_id", ObjectId(questionId)))
                .firstOrNull()
                ?.let { Question.fromDocument(it) }
        } catch (e: Exception) {
            println("Error fetching question: ${e.message}")
            null
        }
    }

    suspend fun deleteQuestion(questionId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val question = questionsCollection
                .find(Filters.eq("_id", ObjectId(questionId)))
                .firstOrNull()
                ?.let { Question.fromDocument(it) }
                ?: return@withContext false

            // Delete the question directly since we don't need to update course references
            val result = questionsCollection.deleteOne(Filters.eq("_id", ObjectId(questionId)))
            return@withContext result.wasAcknowledged()
        } catch (e: Exception) {
            println("Error deleting question: ${e.message}")
            false
        }
    }

    suspend fun editQuestion(questionId: String, userId: String, questionDTO: QuestionDTO): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = Updates.combine(
                Updates.set("title", questionDTO.title),
                Updates.set("description", questionDTO.description),
                Updates.set("files", questionDTO.files),
                Updates.set("section", questionDTO.section),
                Updates.set("lecture", questionDTO.lecture)
            )

            val result = questionsCollection.updateOne(
                Filters.eq("_id", ObjectId(questionId)),
                updates
            )
            return@withContext result.wasAcknowledged()
        } catch (e: Exception) {
            println("Error editing question: ${e.message}")
            false
        }
    }
    suspend fun deleteAnswer(answerId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        val answer = answersCollection
            .find(Filters.eq("_id", ObjectId(answerId)))
            .firstOrNull()
            ?.let { Answer.fromDocument(it) }
            ?: return@withContext false

        if (answer.userId["\$oid"] != userId) return@withContext false

        answersCollection.deleteOne(Filters.eq("_id", ObjectId(answerId))).wasAcknowledged()
    }

    suspend fun editAnswer(answerId: String, userId: String, answerDTO: AnswerDTO): Boolean = withContext(Dispatchers.IO) {
        val answer = answersCollection
            .find(Filters.eq("_id", ObjectId(answerId)))
            .firstOrNull()
            ?: return@withContext false

        if (Answer.fromDocument(answer).userId["\$oid"] != userId) return@withContext false

        val updates = Updates.combine(
            Updates.set("description", answerDTO.description),
            Updates.set("files", answerDTO.files)
        )

        answersCollection.updateOne(Filters.eq("_id", ObjectId(answerId)), updates).wasAcknowledged()
    }

    suspend fun addReply(answerDTO: AnswerDTO, parentAnswerId: String, userId: String): String? = withContext(Dispatchers.IO) {
        val reply = answerDTO.toAnswer(userId, parentAnswerId)
        val replyResult = answersCollection.insertOne(reply.toDocument())
        if (!replyResult.wasAcknowledged()) return@withContext null

        val replyId = replyResult.insertedId?.asObjectId()?.value.toString()
        val parentUpdate = answersCollection.updateOne(
            Filters.eq("_id", ObjectId(parentAnswerId)),
            Updates.push("replies", mapOf("\$oid" to replyId))
        )

        if (parentUpdate.wasAcknowledged()) replyId else null
    }

    suspend fun getReplies(answerId: String): List<Answer> = withContext(Dispatchers.IO) {
        val answer = answersCollection
            .find(Filters.eq("_id", ObjectId(answerId)))
            .firstOrNull()
            ?.let { Answer.fromDocument(it) }
            ?: return@withContext emptyList()

        val replyIds = answer.replies.mapNotNull { it["\$oid"] }
        answersCollection
            .find(Filters.`in`("_id", replyIds.map { ObjectId(it) }))
            .toList()
            .map { Answer.fromDocument(it) }
    }

    suspend fun deleteReply(replyId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        val reply = answersCollection
            .find(Filters.eq("_id", ObjectId(replyId)))
            .firstOrNull()
            ?.let { Answer.fromDocument(it) }
            ?: return@withContext false

        if (reply.userId["\$oid"] != userId) return@withContext false

        answersCollection.deleteOne(Filters.eq("_id", ObjectId(replyId))).wasAcknowledged()
    }

    suspend fun editReply(replyId: String, userId: String, answerDTO: AnswerDTO): Boolean = withContext(Dispatchers.IO) {
        val reply = answersCollection
            .find(Filters.eq("_id", ObjectId(replyId)))
            .firstOrNull()
            ?: return@withContext false

        if (Answer.fromDocument(reply).userId["\$oid"] != userId) return@withContext false

        val updates = Updates.combine(
            Updates.set("description", answerDTO.description),
            Updates.set("files", answerDTO.files)
        )

        answersCollection.updateOne(Filters.eq("_id", ObjectId(replyId)), updates).wasAcknowledged()
    }

        suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
            try {
                usersCollection
                    .find(Filters.eq("_id", ObjectId(userId)))
                    .firstOrNull()
                    ?.let { User.fromDocument(it) }
            } catch (e: Exception) {
                println("Error fetching user: ${e.message}")
                null
            }
        }

}
