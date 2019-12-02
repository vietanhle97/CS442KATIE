package com.example.cs442katie

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.style.BulletSpan
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
import java.lang.Exception
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
        if(!isValidateEmail(email)){
            Toast.makeText(this, "Please input a valid email", Toast.LENGTH_SHORT).show()
        } else if (!isValidateStudentId(studentId)) {
            Toast.makeText(this, "Please input a valid student ID", Toast.LENGTH_SHORT).show()
        } else {
            db.collection("users").whereEqualTo("studentId", studentId).get().addOnSuccessListener {
                if(!it.isEmpty){
                    Toast.makeText(this, "Your ID is already existed. You cannot create another account", Toast.LENGTH_SHORT).show()
                } else {
                    imm.hideSoftInputFromWindow(signup_holder.windowToken, 0)
                    val fragment = RegisterFragment()
                    val bundle = Bundle()
                    bundle.putStringArrayList("user_info", arrayListOf(fullName, studentId, email, password))
                    fragment.arguments = bundle
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right)
                    transaction.replace(R.id.sign_up_activity_content, fragment).addToBackStack(null).commit()
                }

            }
        }
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

    private fun isValidateStudentId(studentId: String) : Boolean{
        Log.e("studentID", studentId)
        try {
            val valid = studentId.toInt()
            if(valid > 20000000 && valid <= (Calendar.getInstance().get(Calendar.YEAR) * 10000) + 9999){
                Log.e("valid", studentId)
                Log.e("hahah", ((Calendar.getInstance().get(Calendar.YEAR) * 10000) + 9999).toString())
                return true
            }
            return false

        } catch (e : Exception) {
            Log.e("cannot covert", "TRUE")
            return false
        }
    }

     override fun onBackPressed() {
         val count = supportFragmentManager.backStackEntryCount
         if(count == 0){
             super.onBackPressed()
         } else {
             supportFragmentManager.popBackStack()
         }
    }
}
