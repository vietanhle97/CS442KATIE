package com.example.cs442katie

data class Course(
    var courseName : String = "CS123",
    var admin: String = "v",
    var instructor: String = "v",
    var student: ArrayList<String> = ArrayList(),
    var lecture: ArrayList<String> = ArrayList()
)