package com.kodea.model.web

import kotlinx.serialization.Serializable

@Serializable
data class TopCategory(
    val title : String,
    val nbCourses : Int = 0,
    val icon : String? = null,
    val color : String? = null
)
