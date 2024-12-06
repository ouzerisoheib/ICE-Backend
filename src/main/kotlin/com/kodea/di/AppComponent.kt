package com.kodea.di

import com.kodea.data.CourseRepoImpl
import com.kodea.data.FileRepoImpl
import com.kodea.data.InstructorRepoImpl
import com.kodea.data.StudentRepoImpl
import com.mongodb.client.MongoClient
import com.mongodb.client.gridfs.GridFSBucket
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MongoModule::class])
interface AppComponent {
    fun studentService(): StudentRepoImpl
    fun instructorService(): InstructorRepoImpl
    fun courseService(): CourseRepoImpl
    fun fileService(): FileRepoImpl
    fun gridFSBucket(): GridFSBucket
    fun mongoClient(): MongoClient
}