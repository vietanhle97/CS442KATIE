package com.example.cs442katie.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cs442katie.Course
import com.example.cs442katie.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges

class MyViewModel : ViewModel() {

    private val userList : MutableLiveData<List<User>> = MutableLiveData()
    private val courseList : MutableLiveData<List<Course>> = MutableLiveData()
    private val currentUser : MutableLiveData<User> = MutableLiveData()
    private val currentCourse : MutableLiveData<Course> = MutableLiveData()
    private val user : MutableLiveData<User> = MutableLiveData()

    fun getUserList() : LiveData<List<User>> {
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { snapshot ->
                userList.postValue(snapshot.toObjects(User::class.java))
            }
        return userList
    }

    fun getCourseList(user: User) : LiveData<List<Course>> {
        FirebaseFirestore.getInstance().collection("courses").get()
            .addOnSuccessListener { snapshot ->
                courseList.postValue(snapshot.filter {
                    it.id in user.course
                }.map { it.toObject(Course::class.java)})
            }
        return courseList
    }

    fun getCurrentUser() : LiveData<User> {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .get().addOnSuccessListener {snapshot ->
            currentUser.postValue(snapshot.toObject(User::class.java))
        }
        return currentUser
    }

    fun getCurrentCourse(courseId : String) : LiveData<Course>{
        FirebaseFirestore.getInstance().collection("courses").document(courseId).get().addOnSuccessListener {snapshot ->
            currentCourse.postValue(snapshot.toObject(Course::class.java))
        }
        return currentCourse
    }

   fun getUserByID(userId : String) : LiveData<User> {
       FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener {snapshot ->
           user.postValue(snapshot.toObject(User::class.java))
       }
       return user
   }
}