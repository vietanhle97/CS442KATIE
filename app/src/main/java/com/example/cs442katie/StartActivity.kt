package com.example.cs442katie

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.ColorFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.activity_start.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class StartActivity : AppCompatActivity() {
    private val PERMISSION_ID = 1
    private val modelFileName = "vargfacenet.tflite"
    lateinit var auth : FirebaseAuth
    lateinit var signin : Button
    lateinit var signup : Button
    lateinit var signin_holder : LinearLayout
    lateinit var circular_progress : ProgressBar
    lateinit var imm : InputMethodManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        if(!checkPermissions()){
            requestPermissions()
        } else{
            startActivitySetup()
        }
    }

    private fun startActivitySetup(){
        findViewById<RelativeLayout>(R.id.sign_in_container).visibility = View.VISIBLE
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        circular_progress  = findViewById(R.id.circular_progress)
        signin_holder = findViewById(R.id.sign_in_holder)
        signin = findViewById(R.id.signin)
        signup = findViewById(R.id.signup)
        auth = FirebaseAuth.getInstance()
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)

        if(!FaceRecognizer.setup(this, assets, modelFileName)) {
            AlertDialog.Builder(this).setMessage("Could not set up the face detector!").setOnDismissListener {
                finish()
            }.show()
            return
        }

        if(FirebaseAuth.getInstance().currentUser != null) {
            signin_holder.visibility = View.GONE
            circular_progress.visibility = View.VISIBLE
            val intent = Intent(this@StartActivity, MainActivity :: class.java)
            startActivity(intent)
            finish()
        }

        signup.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@StartActivity, SignUpActivity :: class.java)
            startActivity(intent)
        })

        email.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(editable: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val empty = charSequence.isNullOrEmpty()
                if(!empty && !password.text.toString().isEmpty()){
                    signIn(email, password)
                } else {
                    signin.setBackgroundResource(R.drawable.button_disable)
                    signin.setOnClickListener(View.OnClickListener {
                        Toast.makeText(this@StartActivity, "Your email and password can't be empty.", Toast.LENGTH_LONG).show()
                    })
                }
            }

        })

        password.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(editable: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val empty = charSequence.isNullOrEmpty()
                if(!empty && email.text.toString().isNotEmpty()){
                    signIn(email, password)
                } else {
                    signin.setBackgroundResource(R.drawable.button_disable)
                    signin.setOnClickListener(View.OnClickListener {
                        Toast.makeText(this@StartActivity, "Your email and password can't be empty.", Toast.LENGTH_SHORT).show()
                    })
                }
            }

        })
    }

    private fun signIn(email: EditText, password: EditText){
        signin.setBackgroundResource(R.drawable.button_gradient)
        signin.setOnClickListener(View.OnClickListener {
            signin.isClickable = true
            signin_holder.visibility = View.GONE
            circular_progress.visibility = View.VISIBLE
            imm.hideSoftInputFromWindow(signin_holder.windowToken, 0)
            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString()).addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(this@StartActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@StartActivity, "Your email or password is incorrect! Please try again", Toast.LENGTH_SHORT).show()
                    signin_holder.visibility = View.VISIBLE
                    circular_progress.visibility = View.GONE
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                }
            }
        })
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.e("check", "not granted")
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_ID
        )
    }

    private fun checkGrantResults(grantResults: IntArray) : Boolean {
        for(i in grantResults){
            if(i != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && checkGrantResults(grantResults))) {
                startActivitySetup()
            } else{
                requestPermissions()
            }
        }else{
            Log.e("result", "permission denied")
        }
    }

}
