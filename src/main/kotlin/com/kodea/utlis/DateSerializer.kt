package com.kodea.utlis

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.Date

object DateSerializer : KSerializer<Date> {
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(formatter.format(value))  // Convert Date to string in ISO format
    }

    override fun deserialize(decoder: Decoder): Date {
        return formatter.parse(decoder.decodeString())  // Parse the string back to Date
    }
}