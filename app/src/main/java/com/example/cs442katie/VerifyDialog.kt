package com.example.cs442katie


import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.admin.DeviceAdminReceiver
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.cs442katie.R
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Frame
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.course_main.*
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import org.w3c.dom.Text
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A simple [Fragment] subclass.
 */
class VerifyDialog : DialogFragment() {
    private val REQUEST_IMAGE_CAPTURE = 2
    lateinit var  camera : ImageView
    lateinit var cameraProgress : ProgressBar
    lateinit var currentPhotoPath: String
    lateinit var bluetoothScanner : BluetoothLeScanner
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var db : FirebaseFirestore
    lateinit var auth : FirebaseAuth
    lateinit var courseId: String
    lateinit var studentId : String
    lateinit var contentView: View
    lateinit var capturedImg : Bitmap

    private val mMessageReceiver = object: BroadcastReceiver() {
        override fun onReceive(context : Context , intent : Intent) {
            contentView.findViewById<RelativeLayout>(R.id.verify_board).visibility = View.INVISIBLE
            contentView.findViewById<ProgressBar>(R.id.camera_progress).visibility = View.GONE
            contentView.findViewById<RelativeLayout>(R.id.verified_board).visibility = View.VISIBLE
        }
    }

    companion object {
        fun newInstance(courseId : String, studentId : String) : VerifyDialog {
            val dialog = VerifyDialog()
            val args = Bundle().apply {
                courseId?.let { putString("courseId", it)
                studentId?.let { putString("studentId", it)}}
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        contentView = activity?.layoutInflater?.inflate(R.layout.fragment_verify_dialog, null) as View

        bluetoothManager = activity!!.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if(arguments?.getString("courseId") != null){
            courseId = arguments?.getString("courseId").toString()
            studentId = arguments?.getString("studentId").toString()
        }
//
        camera = contentView.findViewById(R.id.camera)
        cameraProgress = contentView.findViewById(R.id.camera_progress)
        onClickCameraButton()

        db.collection("courses").document(courseId).addSnapshotListener { result, e ->
            val lecture = result?.get("lecture")
            if(lecture != null){
                if(studentId in (lecture as HashMap<String, Long>).keys){
                    contentView.findViewById<RelativeLayout>(R.id.verify_board).visibility = View.INVISIBLE
                    contentView.findViewById<ProgressBar>(R.id.camera_progress).visibility = View.GONE
                    contentView.findViewById<RelativeLayout>(R.id.verified_board).visibility = View.VISIBLE
                    BLEConnectionSetup()
                }
            }

        }
        builder.setView(contentView)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        LocalBroadcastManager.getInstance(activity!!.applicationContext)
            .registerReceiver(mMessageReceiver,
                IntentFilter("attendanceChecked"))

        db.collection("isCheckingAttendance").document(courseId).addSnapshotListener {
                documentSnapshot, e ->
            if(documentSnapshot?.get("isCheckingAttendance") == false){
                dialog?.dismiss()
            }
        }
        return dialog
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun onClickCameraButton(){
        camera.setOnClickListener(View.OnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
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
                    photoFile?.also { val photoURI: Uri = FileProvider.getUriForFile(context!!, "com.example.cs442katie.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        })

    }

    private fun detectFace(faceImg: Bitmap): Boolean {
        val capturedFace = FaceRecognizer.getFaceBitmap(faceImg)
        if(capturedFace == null) {
            dialog?.dismiss()
            CourseActivity.registration.remove()
            val view = activity?.layoutInflater?.inflate(R.layout.unverified_alert, null) as View
            view.findViewById<TextView>(R.id.attendance_checked_notification).text = "No face found, please try again"
            val dialog = AlertDialog.Builder(activity).setView(view).create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
            return false
        }
        val userFaceFeat = CourseActivity.user.faceFeat.toFloatArray()
        if(!FaceRecognizer.compareFace(capturedFace, userFaceFeat)) {
            dialog?.dismiss()
            CourseActivity.registration.remove()
            val view = activity?.layoutInflater?.inflate(R.layout.unverified_alert, null) as View
            view.findViewById<TextView>(R.id.attendance_checked_notification).text = "Face not matched, please try again"
            val dialog = AlertDialog.Builder(activity).setView(view).create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
//            AlertDialog.Builder(activity).setMessage("Face not matched, please try again").show()
            return false
        }
        return true
    }

    private fun BLEConnectionSetup(){
        val newIntent = Intent(activity as CourseActivity, BlueToothAttendanceCheckerService::class.java)
        newIntent.putExtra("courseId", courseId)
        newIntent.putExtra("studentId", auth.currentUser!!.uid)
        val serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("Disconnected", "TRUE")
            }

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {
                val binder = binder as BlueToothAttendanceCheckerService.LocalBinder
                val blueToothAttendanceCheckerService = binder.getService()
                blueToothAttendanceCheckerService.startScan()
            }
        }

        activity!!.applicationContext.bindService(
            newIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                Log.e("request Code", requestCode.toString())
                Log.e("result Code", resultCode.toString())
                if(resultCode == Activity.RESULT_CANCELED) return
                cameraProgress.visibility = View.VISIBLE
                contentView.findViewById<RelativeLayout>(R.id.verify_board).visibility = View.INVISIBLE
                capturedImg = BitmapFactory.decodeFile(currentPhotoPath) ?: return
                imageProcessing().execute(capturedImg)
            }
        }
    }

    override fun onStop() {
        Log.e("close dialog", "true")
        Log.e("ScanDeviceActivity", "onStop()")
        super.onStop()
    }

    inner class imageProcessing() : AsyncTask<Bitmap, Void, Bitmap>(){

        override fun doInBackground(vararg capturedImg: Bitmap?) : Bitmap{
            Log.e("capturedImg", (capturedImg == null).toString())
            return FaceRecognizer.modifyOrientation(BitmapFactory.decodeFile(currentPhotoPath), currentPhotoPath)
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            if (!detectFace(result!!)) return
            Log.e("Face Recognition", "Matched")
            BLEConnectionSetup()
        }
    }


}
