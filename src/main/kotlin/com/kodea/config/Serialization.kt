package com.kodea.config

//import com.kodea.model.InstantSerializer
import com.kodea.utlis.DateSerializer
import com.kodea.utlis.ObjectIdSerializer
import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bson.types.ObjectId
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = SerializersModule {
                contextual(Date::class, DateSerializer)  // Register your custom serializer
                contextual(ObjectId::class, ObjectIdSerializer)
            }

        })
        gson {
            }
    }

    routing {
        get("/json/kotlinx-serialization") {
                call.respond(mapOf("hello" to "world"))
            }
        get("/json/gson") {
                call.respond(mapOf("hello" to "world"))
            }
    }
}
