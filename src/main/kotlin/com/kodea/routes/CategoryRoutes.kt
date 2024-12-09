package com.kodea.routes

import com.kodea.data.CategoryRepoImpl
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import javax.swing.text.DefaultStyledDocument.ElementSpec.ContentType

fun Route.categoryRoutes(categoryService : CategoryRepoImpl){
    get("/categories") {
        val categories = categoryService.getCategories()
        val json = Json.parseToJsonElement(categories.toString())
        call.respond(json)
    }

}