package com.kodea

import com.kodea.config.*
import com.kodea.di.DaggerAppComponent
import com.kodea.di.MongoModule
import com.kodea.routes.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.routing.*


fun main(args: Array<String>) {
    //io.ktor.server.netty.EngineMain.main(args)

    embeddedServer(
        Netty,
        port = 1234,
        host = "0.0.0.0",
        ){
        module()

    }.start(wait = true)
}

fun Application.module() {
    
    val appComponent = DaggerAppComponent.builder()
        .mongoModule(MongoModule(environment.config))
        .build()
    val studentService = appComponent.studentService()
    val instructorService = appComponent.instructorService()
    val courseService = appComponent.courseService()
    val fileService = appComponent.fileService()
    val reviewService = appComponent.reviewService()
    val categoryService = appComponent.categoryService()
    val gridFSBucket = appComponent.gridFSBucket()
    val mongoClient = appComponent.mongoClient()
    install(PartialContent)
    authentication()
    configureSecurity()
    configureSockets()
    configureSerialization()
    //configureDatabases(studentService,gridFSBucket)
    configureMonitoring()
    configureHTTP()
    configureRouting()
    routing {
        courseRoutes(courseService , fileService , mongoClient)
        studentRoutes(studentService , fileService)
        instructorRoutes(instructorService)
        reviewRoute(reviewService)
        categoryRoutes(categoryService)
    }
}

