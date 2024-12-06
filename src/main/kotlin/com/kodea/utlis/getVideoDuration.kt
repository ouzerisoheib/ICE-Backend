package com.kodea.utlis

import java.io.File

fun getVideoDuration(file: File): Double? {
    return try {
        val process = ProcessBuilder(
            "ffprobe",
            "-i", file.absolutePath,
            "-show_entries", "format=duration",
            "-v", "quiet",
            "-of", "csv=p=0"
        ).redirectErrorStream(true).start()

        process.inputStream.bufferedReader().useLines { lines ->
            lines.firstOrNull()?.toDoubleOrNull() // Parse the duration in seconds
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null // Return null if there's an error
    }
}