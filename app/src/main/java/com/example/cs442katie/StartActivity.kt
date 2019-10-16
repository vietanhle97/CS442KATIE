package com.example.cs442katie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    lateinit var signin : Button
    lateinit var signup : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if(FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this@StartActivity, MainActivity :: class.java)
            startActivity(intent)
            finish()
        }

        signin = findViewById<Button>(R.id.signin)
        signup = findViewById<Button>(R.id.signup)
        auth = FirebaseAuth.getInstance()
        signup.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@StartActivity, SignUpActivity :: class.java)
            startActivity(intent)
        })

        signin.setOnClickListener(View.OnClickListener {
            val email = findViewById<EditText>(R.id.email).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            if(email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this@StartActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else {
                        val message = Toast.makeText(this@StartActivity, "Your email or password is incorrect! Please try again", Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
            } else {
                val message = Toast.makeText(this@StartActivity, "Your email and password can't be empty.", Toast.LENGTH_SHORT)
                message.show()
            }
        })
    }
}
