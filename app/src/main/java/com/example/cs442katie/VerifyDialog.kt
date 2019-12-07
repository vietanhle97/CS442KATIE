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
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteException
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
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.cs442katie.R
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.course_main.*
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class VerifyDialog : DialogFragment() {
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    lateinit var bluetooth : ImageView
    lateinit var  camera : ImageView
    lateinit var cameraProgress : ProgressBar
    private var isRegistered = false
    lateinit var bluetoothScanner : BluetoothLeScanner
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var db : FirebaseFirestore
    lateinit var courseId: String
    lateinit var studentId : String
    lateinit var contentView: View;

    private val mMessageReceiver = object: BroadcastReceiver() {
        override fun onReceive(context : Context , intent : Intent) {
            contentView.findViewById<RelativeLayout>(R.id.verify_board).visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.attendance_checked_notification).visibility = View.VISIBLE
        }
    };

    companion object {
        fun newInstance(courseId : String, studentId : String) : VerifyDialog{
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
        builder.setView(contentView)
        val dialog = builder.create()
        bluetoothManager = activity!!.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
        db = FirebaseFirestore.getInstance()
        if(arguments?.getString("courseId") != null){
            courseId = arguments?.getString("courseId").toString()
            studentId = arguments?.getString("studentId").toString()
        }

        camera = contentView.findViewById(R.id.camera)
        cameraProgress = contentView.findViewById(R.id.camera_progress)
        onClickCameraButton()


        LocalBroadcastManager.getInstance(activity!!.applicationContext)
            .registerReceiver(mMessageReceiver,
                IntentFilter("attendanceChecked"));
        return dialog
    }
//    private fun onClickBluetoothButton(){
//        bluetooth.setOnClickListener(View.OnClickListener {
//            if (!bluetoothAdapter.isEnabled) {
//                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//            } else{
//                bluetooth.visibility = View.GONE
//                scanAvailableBluetooth()
//            }
//        })
//
//    }

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
                Log.e("resuinlt Code", resultCode.toString())
                Log.e("NULL", (data?.extras?.get("data")).toString())
            }


        }

        //suppose checked identity
        val newIntent = Intent(activity!!.applicationContext, BlueToothAttendanceCheckerService ::class.java)
        newIntent.putExtra("courseId", courseId)
        newIntent.putExtra("studentId", studentId)

        val serviceConnection = object : ServiceConnection{
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.e("Disonnected", "TRUE")
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val binder = binder as BlueToothAttendanceCheckerService.LocalBinder
                val blueToothAttendanceCheckerService = binder.getService()
                blueToothAttendanceCheckerService.startScan()
            }

        }

        activity!!.applicationContext.bindService(newIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    }


    override fun onStop() {
        Log.e("close dialog", "true")
        Log.e("ScanDeviceActivity", "onStop()")
        super.onStop()
    }
}
