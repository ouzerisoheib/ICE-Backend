package com.kodea.model.web

import com.kodea.model.Level
import kotlinx.serialization.Serializable

@Serializable
data class FeatureCourse(
    val title : String,
    val image : String,
    val category : String,
    val price : Int,
    val discount : Int,
    val instructorName : String,
    val instructorImage : String,
    val instructorRating : Double,
    val level : Level,
    val duration : Long,
    val nbEnrolledStudents : Int,
)
