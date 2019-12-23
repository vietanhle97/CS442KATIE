package com.example.cs442katie

import android.app.Activity
import android.app.AlertDialog
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import java.io.File
import java.util.HashMap

class CourseActivity : AppCompatActivity() {
    lateinit var db : FirebaseFirestore
    private val modelFileName = "vargfacenet.tflite"
    lateinit var auth: FirebaseAuth
    lateinit var courseId : String
    lateinit var studentId : String
    lateinit var adminId: String

    companion object {
        var user: User = User()
        var registration = ListenerRegistration {  }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)
        val toolbar: Toolbar = findViewById(R.id.courseToolbar)
        toolbar.title = intent.extras?.getString("courseName")
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        auth = FirebaseAuth.getInstance()
        courseId = intent.extras?.getString("courseId") as String
        studentId = intent.extras?.getString("studentId") as String
        adminId = intent.extras?.getString("adminId") as String

        if(!FaceRecognizer.setup(this, assets, modelFileName)) {
            AlertDialog.Builder(this).setMessage("Could not set up the face detector!").setOnDismissListener {
                finish()
            }.show()
            return
        }

        Log.e("courseId", courseId)
        Log.e("studentId", studentId)
        Log.e("adminId", adminId)



        val isAdmin = (adminId == auth.currentUser!!.uid)
        val button_verify = findViewById<Button>(R.id.button_verify)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        db.collection("users").document(auth.uid!!).get().addOnSuccessListener { result ->
            user = result.toObject(User::class.java)!!}

        if(isAdmin == false){
            if(courseId != null){
                button_verify.setOnClickListener(View.OnClickListener {
                    val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                    verifyDialog.show(supportFragmentManager, "VerifyDialog")
                })
                val query = db.collection("isCheckingAttendance").document(courseId)
                registration = query.addSnapshotListener {
                        documentSnapshot, e ->
                    if(documentSnapshot!!.get("isCheckingAttendance") == true) {
                        val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                        verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        button_verify.visibility = View.VISIBLE
                    } else{
                        button_verify.visibility = View.GONE
                    }
                }
            }
        }
        db.collection("courses").document(courseId).get().addOnSuccessListener {
                result->
            val studentList = result?.get("student") as ArrayList<String>
            val todayAttendance = HashMap<String, Boolean>()
            if(result.get("lecture") == null){
                for (i in studentList){
                    if(i != adminId){
                        todayAttendance[i] = false
                    }
                }
            } else{
                val lectureList = result.get("lecture") as HashMap<String, Long>
                for (i in studentList){
                    if(i != adminId) {
                        FirebaseFirestore.getInstance().collection("users").document(i).get().addOnSuccessListener { result ->
                            val course = result.get("course") as HashMap<String, Long>
                            todayAttendance[i] = course[courseId] == 1L
                        }

                    }
                }
            }
            val userDb = FirebaseFirestore.getInstance().collection("users").get()
            userDb.addOnSuccessListener {result ->
                val userList = result.filter {
                    it.id in studentList && it.id != adminId
                }.map {
                    it.toObject(User::class.java)
                }
                val attendanceListAdapter = AttendanceListAdapter(this@CourseActivity, userList, courseId, todayAttendance, isAdmin)
                val recycler = findViewById<RecyclerView>(R.id.student_list)
                recycler.setHasFixedSize(true)

                recycler.layoutManager = LinearLayoutManager(this@CourseActivity)
                recycler.adapter = attendanceListAdapter
            }

        }


        db.collection("courses").document(courseId).addSnapshotListener(MetadataChanges.INCLUDE){
            documentSnapshot, firebaseFirestoreException ->
            Log.e("real time?", "REAL TIME")
        }
    }

    override fun onResume() {
        super.onResume()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if(!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
        }

    }


    override fun onPause() {
        super.onPause()
        registration.remove()
    }
}
