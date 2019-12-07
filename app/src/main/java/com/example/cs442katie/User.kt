package com.example.cs442katie

data class User(
    var course : HashMap<String, Int> = HashMap(),
    var email : String = "",
    var fullName : String = "",
    var studentId : String = "",
    var id : String = ""
)