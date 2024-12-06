package com.kodea.model.utils

import kotlinx.serialization.Serializable
import java.io.InputStream


data class Video(
    val fileName: String,
    val inputStream: InputStream,
    val contentType: String
)
