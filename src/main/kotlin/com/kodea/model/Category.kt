package com.kodea.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val title : String,
    val subCategories : List<SubCategory>,
    val color : String,
    val icon : String
)

@Serializable
data class SubCategory(
    val title : String,
    val tools : List<String>
)