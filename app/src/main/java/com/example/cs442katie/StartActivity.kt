package com.example.cs442katie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        var signin = findViewById<Button>(R.id.signin)
        var signup = findViewById<Button>(R.id.signup)

        signup.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@StartActivity, SignUpActivity :: class.java)
            startActivity(intent)
        })
    }
}
