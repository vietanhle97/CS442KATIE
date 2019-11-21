package com.example.cs442katie

import android.hardware.biometrics.BiometricManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.DialogTitle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.example.cs442katie.VerifyDialog

class CourseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course)
        val toolbar: Toolbar = findViewById(R.id.courseToolbar)

        toolbar.title = intent.extras?.getString("courseName")
        Log.e("course", intent.extras?.getString("courseName"))
        setSupportActionBar(toolbar)
        val verifyDialog = VerifyDialog()
        verifyDialog.show(supportFragmentManager, "VerifyDialog")
    }
}
