package com.kodea.model.web

import com.kodea.model.Level

data class RecentCourse(
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
