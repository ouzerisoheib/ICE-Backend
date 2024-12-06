package com.kodea.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val title : String,
    val subCategories : List<String>,
    val color : String,
)