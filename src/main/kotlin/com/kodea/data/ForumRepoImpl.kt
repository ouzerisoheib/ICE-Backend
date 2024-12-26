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
    init{
        database.createCollection("courses")
        questionsCollection = database.getCollection("questions")
        coursesCollection = database.getCollection("courses")
        answersCollection = database.getCollection("answers")
        votesCollection = database.getCollection("votes")
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
}