package com.example.cs442katie

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey var id : String = "",
    var course : HashMap<String, Long> = HashMap(),
    var email : String = "",
    var fullName : String = "",
    var studentId : String = "",
    var faceFeat : ArrayList<Float> = ArrayList(),
    var faceUri: String = "",
    var currentClassCount : HashMap<String, Int> = HashMap()
)

