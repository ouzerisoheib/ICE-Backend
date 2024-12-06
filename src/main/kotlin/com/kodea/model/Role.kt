package com.kodea.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    Student,
    Instructor,
    Admin
}