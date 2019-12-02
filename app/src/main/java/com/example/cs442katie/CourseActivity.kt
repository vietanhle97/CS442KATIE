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
import com.example.cs442katie.VerifyDialog
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import java.io.File
import java.util.HashMap

class CourseActivity : AppCompatActivity() {
    lateinit var db : FirebaseFirestore
    private val REQUEST_IMAGE_CAPTURE = 100
    lateinit var courseId : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)
        val toolbar: Toolbar = findViewById(R.id.courseToolbar)

        toolbar.title = intent.extras?.getString("courseName")
        courseId = intent.extras?.getString("courseId") as String
        val isAdmin = intent.extras?.getBoolean("isAdmin")
        val button_verify = findViewById<Button>(R.id.button_verify)


        db = FirebaseFirestore.getInstance()
        setSupportActionBar(toolbar)
        button_verify.visibility = View.VISIBLE
        if(isAdmin == false){
            if(courseId != null){
                db.collection("courses").document(courseId).get().addOnSuccessListener { result ->
                    if(result.get("isCheckingAttendance") == true){
                        val verifyDialog = VerifyDialog.newInstance(courseId)
                        verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        button_verify.visibility = View.VISIBLE
                        button_verify.setOnClickListener(View.OnClickListener {
                            val verifyDialog = VerifyDialog.newInstance(courseId)
                            verifyDialog.show(supportFragmentManager, "VerifyDialog")
                        })
                    }
                }
            }
        }
    }
}
