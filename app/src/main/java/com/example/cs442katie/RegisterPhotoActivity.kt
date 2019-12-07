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
        var faceImg = faceImg
        faceImg = faceImg.copy(Bitmap.Config.ARGB_8888, true)
        val imageView = findViewById<ImageView>(R.id.please_take_picture)
        val faceDetector = FaceDetector.Builder(this).setTrackingEnabled(false).build()
        if (!faceDetector.isOperational) {
            AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show()
            return
        }
        val frame = Frame.Builder().setBitmap(faceImg).build()
        val faces = faceDetector.detect(frame)

        if (faces.size() == 0) {
            Toast.makeText(this, "No face found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val face = faces.valueAt(0)
        var capturedFace =
            Bitmap.createBitmap(face.width.toInt(), face.height.toInt(), Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(capturedFace)
        tempCanvas.drawBitmap(faceImg, -face.position.x, -face.position.y, null)
        imageView.setImageBitmap(capturedFace)
        capturedFace = FaceRecognizer.getResizedBitmap(capturedFace)
        val baos = ByteArrayOutputStream()
        capturedFace.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val faceRef = mStorageRef.child("$studentId.jpg")
        var uploadTask = faceRef.putBytes(data)
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Log.e("FireStorage", "Upload failure")
        }.addOnSuccessListener {
            var faceFeat = FaceRecognizer.addFaceBitmap(capturedFace, studentId)
            auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if(it.isSuccessful) {
                    val user = auth.currentUser
                    val id = user!!.uid
                    val newUser = hashMapOf(
                        "fullName" to fullName,
                        "studentId" to studentId,
                        "email" to email,
                        "course" to hashMapOf("CS442" to 0, "CS489" to 0),
                        "faceUri" to faceRef.path,
                        "faceFeat" to faceFeat
                    )
                    db.collection("users").document(id).set(newUser).addOnSuccessListener {
                        // We auto enroll every students to the CS442 course.
                        db.collection("courses").document("CS442").update("student", FieldValue.arrayUnion(id))
                        val intent = Intent(applicationContext, MainActivity :: class.java)
                        startActivity(intent)
                    }
                } else {
                    val message = Toast.makeText(applicationContext, "Register Failed. Please try again", Toast.LENGTH_SHORT)
                    message.show()
//                        circular_progress.visibility = View.GONE
//                        signup_holder.visibility = View.VISIBLE
//                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                }
            }
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
                    capturedImg = modifyOrientation(capturedImg, currentPhotoPath)
                    Log.e("img", "okayyy")
                    detectFace(capturedImg)
                }

            }
        }
    }

    private fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String): Bitmap {
        var ei: ExifInterface? = null
        try {
            ei = ExifInterface(image_absolute_path)
        } catch (e: Exception) {
            return bitmap
        }

        val orientation =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return rotate(bitmap, 90f)

            ExifInterface.ORIENTATION_ROTATE_180 -> return rotate(bitmap, 180f)

            ExifInterface.ORIENTATION_ROTATE_270 -> return rotate(bitmap, 270f)

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> return flip(bitmap, true, false)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> return flip(bitmap, false, true)

            else -> return bitmap
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
