package com.kodea.model

import kotlinx.serialization.Serializable

@Serializable
data class Lecture(
    val name : String,
    val description : String,
    val caption : String,
    val note : String,
    val video : String,
    val file : String?
)
