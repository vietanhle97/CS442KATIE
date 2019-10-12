package com.example.cs442katie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        var register_button = findViewById<Button>(R.id.register_btn)
        var username = findViewById<EditText>(R.id.username) as EditText
        var email = findViewById<EditText>(R.id.email)
        var password = findViewById<EditText>(R.id.password)
        register_button.setOnClickListener(View.OnClickListener {
            SignUp(username.text.toString(), email.text.toString(), password.text.toString())
        })
    }

    fun SignUp(username : String, email : String, password : String){
        auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            OnCompleteListener {
                fun onComplete(task : Task<AuthResult>){
                    if(task.isSuccessful){
                        var user = auth.currentUser
                        var id = user?.uid
                        var reference = FirebaseDatabase.getInstance().reference.child("Users").child(id!!)

                        var hashmap : HashMap<String, String> = HashMap()
                        hashmap.put("username", username)

                        reference.setValue(hashmap).addOnCompleteListener(OnCompleteListener {
                            fun onComplete(task : Task<Void>){
                                if(task.isSuccessful){
                                    var intent = Intent(this@SignUpActivity, MainActivity :: class.java)
                                    startActivity(intent)

                                }
                            }
                        })
                    }
                }
            })

    }
}
