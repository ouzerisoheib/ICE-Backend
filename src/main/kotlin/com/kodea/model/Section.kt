package com.kodea.model

import kotlinx.serialization.Serializable

@Serializable
data class Section(
    val title : String,
    val lectures : List<Lecture>
)
