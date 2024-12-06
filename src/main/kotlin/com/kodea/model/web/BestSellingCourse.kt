package com.kodea.model.web

import kotlinx.serialization.Serializable

@Serializable
data class BestSellingCourse(
    val title : String,
    val image : String,
    val category : String,
    val price : Int,
    val discount : Int,
    val rating : Double,
    val nbEnrolledStudents : Int,
)
