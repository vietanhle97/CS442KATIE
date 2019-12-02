package com.example.cs442katie

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    lateinit var db : FirebaseFirestore
    lateinit var circular_progress : ProgressBar
    lateinit var imm : InputMethodManager
    lateinit var signup_holder : LinearLayout
    lateinit var registerButton : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        db = FirebaseFirestore.getInstance()
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        circular_progress  = findViewById(R.id.circular_progress)
        signup_holder = findViewById(R.id.sign_up_holder)
        registerButton = findViewById<Button>(R.id.next_button)
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
        if(isValidateEmail(email)){
            db.collection("users").whereEqualTo("studentId", studentId).get().addOnSuccessListener {
                if(!it.isEmpty){
                    Toast.makeText(this, "Your ID is already existed. You cannot create another account", Toast.LENGTH_SHORT).show()
                } else {
                    val fragment = RegisterFragment()
                    val transaction = supportFragmentManager.beginTransaction().replace(R.id.sign_up_activity_content, fragment)
                    transaction.commit()
                }

            }
        } else {
            Toast.makeText(this, "Please input a valid email", Toast.LENGTH_SHORT).show()
        }


//        auth = FirebaseAuth.getInstance()
//        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
//            if(it.isSuccessful) {
//                val user = auth.currentUser
//                val id = user!!.uid
//                val newUser = hashMapOf(
//                    "fullName" to fullName,
//                    "studentId" to studentId,
//                    "email" to email,
//                    "course" to arrayListOf("CS442", "CS489")
//                )
//                db.collection("users").document(id).set(newUser).addOnSuccessListener {
//                    // We auto enroll every students to the CS442 course.
//                    db.collection("courses").document("CS442").update("student", FieldValue.arrayUnion(id))
//                    val intent = Intent(this@SignUpActivity, MainActivity :: class.java)
//                    startActivity(intent)
//                    finish()
//                }
//            } else {
//                val message = Toast.makeText(this@SignUpActivity, "Register Failed. Please try again", Toast.LENGTH_SHORT)
//                message.show()
//                circular_progress.visibility = View.GONE
//                signup_holder.visibility = View.VISIBLE
//                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
//            }
//        }
    }

    private fun isValidateEmail(email : String) : Boolean{
        val regExpression = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@" + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\." + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|" + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        val inputString : CharSequence = email
        val pattern = Pattern.compile(regExpression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(inputString)
        if(matcher.matches()){
            return true
        }
        return false
    }

     override fun onBackPressed() {
        super.onBackPressed()
         supportFragmentManager.popBackStack()
    }


}
