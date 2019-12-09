package com.example.cs442katie

data class User(
    var course : HashMap<String, Int> = HashMap(),
    var email : String = "",
    var fullName : String = "",
    var studentId : String = "",
    var faceFeat : ArrayList<Float> = ArrayList(),
    var id : String = "",
    var faceUri: String = "",
    var currentClassCount : HashMap<String, Int> = HashMap()
)