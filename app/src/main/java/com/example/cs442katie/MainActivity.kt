package com.example.cs442katie

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.auth.Token
import com.google.firebase.iid.FirebaseInstanceId

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var btn = findViewById<Button>(R.id.check_attendace)
        btn.setOnClickListener(View.OnClickListener {

        })

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token as String

                val userId = FirebaseAuth.getInstance().uid as String

                FirebaseDatabase.getInstance().reference.child("token").child(userId).setValue(token)

                // Log and toast
//                val msg = getString(R.string.msg_token_fmt, token)
//                Log.d("current token", msg)
//                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })
    }
}
