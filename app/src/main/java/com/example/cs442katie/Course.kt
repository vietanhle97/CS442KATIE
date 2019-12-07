package com.example.cs442katie

data class Course(
    var UUID : String = "DITMEASDADS",
    var courseName : String = "Intro to CS",
    val courseId: String = "CS123",
    var admin: String = "v",
    var instructor: String = "v",
    var student: ArrayList<String> = ArrayList(),
    var lecture: ArrayList<HashMap<String, Long> > = ArrayList()
)