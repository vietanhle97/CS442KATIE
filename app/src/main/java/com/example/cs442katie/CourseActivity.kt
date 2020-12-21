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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cs442katie.VerifyDialog
import com.example.cs442katie.databinding.ActivityCourseBinding
import com.example.cs442katie.models.MyViewModel
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.course_main.*
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import java.io.File
import java.util.HashMap

class CourseActivity : AppCompatActivity() {
    lateinit var db : FirebaseFirestore
    lateinit var courseBinding: ActivityCourseBinding
    lateinit var model: MyViewModel
    private val modelFileName = "vargfacenet.tflite"
    lateinit var auth: FirebaseAuth
    lateinit var courseId : String
    lateinit var studentId : String
    lateinit var adminId: String

    companion object {
        var user: User = User()
        var registration = ListenerRegistration {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        courseBinding = DataBindingUtil.setContentView(this, R.layout.activity_course)
        model = ViewModelProviders.of(this).get(MyViewModel::class.java)

        courseBinding.courseToolbar.title = intent.extras?.getString("courseName")
        setSupportActionBar(courseBinding.courseToolbar)
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

        val isAdmin = (adminId == auth.currentUser!!.uid)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        db.collection("users").document(auth.uid!!).get().addOnSuccessListener { result ->
            user = result.toObject(User::class.java)!!}

        if(isAdmin == false){
            if(courseId != null){
                courseBinding.buttonVerify.setOnClickListener(View.OnClickListener {
                    val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                    verifyDialog.show(supportFragmentManager, "VerifyDialog")
                })
                val query = db.collection("isCheckingAttendance").document(courseId)
                registration = query.addSnapshotListener {
                        documentSnapshot, e ->
                    if(documentSnapshot!!.get("isCheckingAttendance") == true) {
                        val verifyDialog = VerifyDialog.newInstance(courseId, studentId)
                        verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        courseBinding.buttonVerify.visibility = View.VISIBLE
                    } else{
                        courseBinding.buttonVerify.visibility = View.GONE
                    }
                }
            }
        }

        model.getCurrentCourse(courseId).observe(this, Observer { result ->
            val todayAttendance = HashMap<String, Boolean>()
            if(result.lecture.isEmpty()){
                for (i in result.student){
                    if(i != adminId){
                        todayAttendance[i] = false
                    }
                }
            } else{
                for (i in result.student){
                    if(i != adminId) {
                        model.getUserByID(i).observe(this, Observer {result ->
                            val course = result.course
                            todayAttendance[i] = course[courseId] == 1L
                        })

                    }
                }
            }

            model.getUserList().observe(this, Observer { userList ->
                val attendanceListAdapter = AttendanceListAdapter(this@CourseActivity, userList.filter { user ->
                    user.id != adminId
                }, courseId, todayAttendance, isAdmin)

                courseBinding.studentRecycler.setHasFixedSize(true)

                courseBinding.studentRecycler.layoutManager = LinearLayoutManager(this@CourseActivity)
                courseBinding.studentRecycler.adapter = attendanceListAdapter
            })
        })

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
