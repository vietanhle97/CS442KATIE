package com.example.cs442katie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        db = FirebaseFirestore.getInstance()

        val registerButton = findViewById<Button>(R.id.register_btn)
        val fullName = findViewById<EditText>(R.id.full_name) as EditText
        val studentId = findViewById<EditText>(R.id.student_id) as EditText
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        registerButton.setOnClickListener(View.OnClickListener {
            if (fullName.text.toString().isEmpty()
                || email.text.toString().isEmpty()
                || password.text.toString().isEmpty()
                || studentId.text.toString().isEmpty()) {

                val message = Toast.makeText(this@SignUpActivity, "Please fill in all required field", Toast.LENGTH_SHORT)
                message.show()
            } else{
                signUp(fullName.text.toString(), studentId.text.toString(), email.text.toString(), password.text.toString())
            }
        })
    }

    private fun signUp(fullName : String, studentId : String, email : String, password : String) {
        auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if(it.isSuccessful) {
                val user = auth.currentUser
                val id = user!!.uid

                val newUser = hashMapOf(
                    "fullName" to fullName,
                    "studentId" to studentId,
                    "email" to email,
                    "course" to arrayListOf("CS442", "CS489")
                )
                db.collection("users").document(id).set(newUser).addOnSuccessListener {
                    // We auto enroll every students to the CS442 course.
                    db.collection("courses").document("CS442").update("student", FieldValue.arrayUnion(id))
                    val intent = Intent(this@SignUpActivity, MainActivity :: class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                val message = Toast.makeText(this@SignUpActivity, "Register Failed. Please try again", Toast.LENGTH_SHORT)
                message.show()
            }
        }


    }
}
