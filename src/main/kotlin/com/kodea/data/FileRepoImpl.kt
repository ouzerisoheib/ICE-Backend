package com.kodea.data

import com.kodea.utlis.getVideoDuration
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import java.io.File
import java.io.InputStream

class FileRepoImpl(private val gridFSBucket: GridFSBucket) {
    suspend fun uploadFile(fileName: String, inputStream: InputStream, contentType: String): Pair<String, Double?> =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("upload_", fileName)
            tempFile.deleteOnExit()
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Calculate the duration of the video
            val duration = getVideoDuration(tempFile)

            val metadata = Document("contentType", contentType)
            if (duration != null) {
                metadata.append("duration", duration)
            }

            val uploadOptions = GridFSUploadOptions().chunkSizeBytes(1024 * 255)
                .metadata(metadata)

            val fileId = gridFSBucket.uploadFromStream(fileName, tempFile.inputStream(), uploadOptions).toString()
            tempFile.delete()
            fileId to duration
        }

    // Function to download file from GridFS
    /*fun downloadFile(fileId: ObjectId): File? {
        val gridDownloadStream = gridFSBucket.openDownloadStream(fileId)
        val fileName = gridDownloadStream.gridFSFile.filename
        val contentType = gridDownloadStream.gridFSFile.metadata?.getString("contentType") ?:"application/octet-stream"
        ContentType.parse(contentType)
        val file = gridFSBucket.find().firstOrNull().
        return file
    }*/
    suspend fun getFile(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = gridFSBucket.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
            ?: return@withContext null
        val downloadStream = gridFSBucket.openDownloadStream(file.id)
        downloadStream.readBytes()
    }
}