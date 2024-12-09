package com.kodea.di

import com.kodea.data.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import dagger.Module
import dagger.Provides
import io.ktor.server.config.*
import javax.inject.Singleton

@Module
class MongoModule(private val config: ApplicationConfig) {

    @Provides
    @Singleton
    fun provideMongoClient(): MongoClient {
        val host = config.propertyOrNull("db.mongo.host")?.getString() ?: "127.0.0.1"
        val port = config.propertyOrNull("db.mongo.port")?.getString() ?: "27017"
        val maxPoolSize = config.propertyOrNull("db.mongo.maxPoolSize")?.getString()?.toInt() ?: 20
        val uri = "mongodb://$host:$port/?maxPoolSize=$maxPoolSize&w=majority"
        return MongoClients.create(uri)
    }

    @Provides
    @Singleton
    fun provideDatabase(client: MongoClient): MongoDatabase {
        val databaseName = config.propertyOrNull("db.mongo.database.name")?.getString() ?: "myDatabase"
        return client.getDatabase(databaseName)
    }

    @Provides
    @Singleton
    fun provideStudentService(database: MongoDatabase): StudentRepoImpl {
        return StudentRepoImpl(database)
    }
    @Provides
    @Singleton
    fun provideInstructorService(database: MongoDatabase): InstructorRepoImpl {
        return InstructorRepoImpl(database)
    }

    @Provides
    @Singleton
    fun provideCourseRepoImpl(database: MongoDatabase): CourseRepoImpl {
        return CourseRepoImpl(database)
    }

    @Provides
    @Singleton
    fun provideReviewRepoImpl(database: MongoDatabase): ReviewRepoImpl {
        return ReviewRepoImpl(database)
    }

    @Provides
    @Singleton
    fun provideCategoryRepoImpl(database: MongoDatabase): CategoryRepoImpl {
        return CategoryRepoImpl(database)
    }

    @Provides
    @Singleton
    fun provideFileRepoImpl(gridFSBucket: GridFSBucket): FileRepoImpl {
        return FileRepoImpl(gridFSBucket)
    }

    @Provides
    @Singleton
    fun provideGridFSBucket(database: MongoDatabase): GridFSBucket {
        return GridFSBuckets.create(database)
    }
}