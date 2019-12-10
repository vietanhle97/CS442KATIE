package com.example.cs442katie

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterPhotoActivity : AppCompatActivity() {
    private val REQUEST_IMAGE_CAPTURE  = 2
    lateinit var camera : ImageView
    lateinit var mStorageRef: StorageReference
    lateinit var db : FirebaseFirestore
    lateinit var auth : FirebaseAuth
    private lateinit var currentPhotoPath: String
    private lateinit var fullName : String
    private lateinit var studentId : String
    private lateinit var email : String
    private lateinit var password : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_photo)
        fullName = intent.extras!!.getString("fullName")!!
        studentId = intent.extras!!.getString("studentId")!!
        email = intent.extras!!.getString("email")!!
        password = intent.extras!!.getString("password")!!
        camera = findViewById(R.id.camera)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        mStorageRef = FirebaseStorage.getInstance().reference
        onClickCameraButton()
    }

    private fun onClickCameraButton(){
        camera.setOnClickListener(View.OnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e("Register Fragment", "Can't open photo file")
                        null
                    }
                    Log.e("file", currentPhotoPath)
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.cs442katie.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        })

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun detectFace(faceImg: Bitmap) {
        findViewById<RelativeLayout>(R.id.register_template).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.circular_progress).visibility = View.VISIBLE

        val capturedFace = FaceRecognizer.getFaceBitmap(faceImg)
        if(capturedFace == null) {
            Toast.makeText(this, "No face found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageView = findViewById<ImageView>(R.id.please_take_picture)
        imageView.setImageBitmap(capturedFace)

        val baos = ByteArrayOutputStream()
        capturedFace.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val faceRef = mStorageRef.child("$studentId.jpg")
        var uploadTask = faceRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            Log.e("success", "success")
            var faceFeat = FaceRecognizer.getFaceFeat(capturedFace)
            auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if(it.isSuccessful) {
                    val user = auth.currentUser
                    val id = user!!.uid
                    val newUser = hashMapOf(
                        "id" to id,
                        "fullName" to fullName,
                        "studentId" to studentId,
                        "email" to email,
                        "course" to hashMapOf("CS442" to 0, "CS489" to 0, "CS459" to 0),
                        "faceUri" to faceRef.path,
                        "faceFeat" to faceFeat.toCollection(ArrayList())
                    )
                    db.collection("users").document(id).set(newUser).addOnSuccessListener {
                        Log.e("success", "here")
                        // We auto enroll every students to the CS442 course.
                        db.collection("courses").document("CS442").update("student", FieldValue.arrayUnion(id))
                        db.collection("courses").document("CS489").update("student", FieldValue.arrayUnion(id))
                        db.collection("courses").document("CS459").update("student", FieldValue.arrayUnion(id))
                        val intent = Intent(this, MainActivity :: class.java)
                        startActivity(intent)
                    }.addOnFailureListener {
                        Log.e("dtb push fail", it.toString())
                    }
                } else {
                    val message = Toast.makeText(applicationContext, "Register Failed. Please try again", Toast.LENGTH_SHORT)
                    message.show()
//                        circular_progress.visibility = View.GONE
//                        signup_holder.visibility = View.VISIBLE
//                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                }
            }
        }.addOnFailureListener {
            Log.e("upload fail", it.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_IMAGE_CAPTURE -> {
                Log.e("request Code", requestCode.toString())
                Log.e("result Code", resultCode.toString())
                Log.e("NULL", (data?.extras?.get("data")).toString())
                if(currentPhotoPath != null) {
                    var capturedImg = BitmapFactory.decodeFile(currentPhotoPath)
                    capturedImg = FaceRecognizer.modifyOrientation(capturedImg, currentPhotoPath)
                    Log.e("img", "okayyy")
                    detectFace(capturedImg)
                }

            }
        }
    }
}
