package com.example.cs442katie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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
        val register_button = findViewById<Button>(R.id.register_btn)
        val username = findViewById<EditText>(R.id.username) as EditText
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        register_button.setOnClickListener(View.OnClickListener {
            SignUp(username.text.toString(), email.text.toString(), password.text.toString())
        })
    }

    private fun SignUp(username : String, email : String, password : String){
        auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            OnCompleteListener {
                if(it.isSuccessful){
                    val user = auth.currentUser
                    val id = user!!.uid
                    val reference = FirebaseDatabase.getInstance().reference.child("Users").child(id)

                    val hashmap : HashMap<String, String> = HashMap()
                    hashmap.put("username", username)
                    hashmap.put("id", id)
                    reference.setValue(hashmap).addOnCompleteListener(OnCompleteListener {
                        if(it.isSuccessful){
                            val intent = Intent(this@SignUpActivity, MainActivity :: class.java)
                            startActivity(intent)
                            finish()
                        }
                    })
                } else{
                    val message = Toast.makeText(this@SignUpActivity, "Authentication failed. Please try again", Toast.LENGTH_SHORT)
                    message.show()
                }
            })

    }
}
