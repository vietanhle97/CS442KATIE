package com.example.cs442katie


import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A simple [Fragment] subclass.
 */
class RegisterFragment : Fragment() {
    private val REQUEST_IMAGE_CAPTURE  = 2
    lateinit var camera : ImageView
    lateinit var db : FirebaseFirestore
    lateinit var auth : FirebaseAuth
    private lateinit var fullName : String
    private lateinit var studentId : String
    private lateinit var email : String
    private lateinit var password : String
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val bundle = arguments?.getStringArrayList("user_info")
        if(bundle != null){
            fullName = bundle[0]
            studentId = bundle[1]
            email = bundle[2]
            password = bundle[3]
        }
        val mainView =  inflater.inflate(R.layout.fragment_register, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        camera = mainView.findViewById(R.id.camera)
        onClickCameraButton()
        return mainView

    }

    private fun onClickCameraButton(){
        camera.setOnClickListener(View.OnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if(takePictureIntent.resolveActivity(activity!!.packageManager) != null){
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_IMAGE_CAPTURE -> {
                Log.e("request Code", requestCode.toString())
                Log.e("result Code", resultCode.toString())
                Log.e("NULL", (data?.extras?.get("data")).toString())
                if(data?.extras?.get("data") != null){
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
                                val intent = Intent(activity!!.applicationContext, MainActivity :: class.java)
                                startActivity(intent)
                            }
                        } else {
                            val message = Toast.makeText(activity!!.applicationContext, "Register Failed. Please try again", Toast.LENGTH_SHORT)
                            message.show()
//                        circular_progress.visibility = View.GONE
//                        signup_holder.visibility = View.VISIBLE
//                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                        }
                    }
                }

            }
        }
    }
}
