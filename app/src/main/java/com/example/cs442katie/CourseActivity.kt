package com.example.cs442katie

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.DialogTitle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs442katie.VerifyDialog
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import java.io.File
import java.util.HashMap

class CourseActivity : AppCompatActivity() {
    lateinit var db : FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var courseId : String
    lateinit var studentId : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)
        val toolbar: Toolbar = findViewById(R.id.courseToolbar)

        toolbar.title = intent.extras?.getString("courseName")
        courseId = intent.extras?.getString("courseId") as String
        studentId = intent.extras?.getString("studentId") as String
        val isAdmin = intent.extras?.getBoolean("isAdmin")
        val button_verify = findViewById<Button>(R.id.button_verify)


        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setSupportActionBar(toolbar)
        if(isAdmin == false){
            if(courseId != null){
                db.collection("courses").document(courseId).get().addOnSuccessListener { result ->
                    if(result.get("isCheckingAttendance") == true){
                        val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                        verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        button_verify.visibility = View.VISIBLE
                        button_verify.setOnClickListener(View.OnClickListener {
                            val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                            verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        })
                    }
                }
            }
        }
        db.collection("courses").document(courseId).get().addOnSuccessListener { result ->
            val studentList = result.get("student") as ArrayList<String>
            val lectureList = result.get("lecture") as ArrayList<HashMap<String, Long>>
            val todayLecture = lectureList[lectureList.size - 1]
            val todayAttendance = HashMap<String, Boolean>()
            for(i in todayLecture.keys)
                Log.e("student", i)
            for (i in studentList){
                Log.e("student and keys", i.toString() + " " + (i in todayLecture.keys).toString())
                todayAttendance[i] = (i in todayLecture.keys)
            }

            val userDb = FirebaseFirestore.getInstance().collection("users").get()
            userDb.addOnSuccessListener {result ->
                val userList = result.filter {
                    it.id in studentList
                }.map {
                    it.toObject(User::class.java)
                }
                val attendanceListAdapter = AttendanceListAdapter(this@CourseActivity, userList, courseId, todayAttendance)
                val recycler = findViewById<RecyclerView>(R.id.student_list)
                recycler.setHasFixedSize(true)

                recycler.layoutManager = LinearLayoutManager(this@CourseActivity)
                recycler.adapter = attendanceListAdapter
            }
        }
    }
}
