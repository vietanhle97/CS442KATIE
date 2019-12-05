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
import android.os.Parcelable
import android.os.RemoteException
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.DialogFragment

import com.example.cs442katie.R
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.course_main.*
import kotlinx.android.synthetic.main.fragment_verify_dialog.*
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
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
    lateinit var bluetoothProgress : ProgressBar
    lateinit var  camera : ImageView
    lateinit var cameraProgress : ProgressBar
    private var isRegistered = false
    lateinit var bluetoothScanner : BluetoothLeScanner
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var items : String
    lateinit var db : FirebaseFirestore

    companion object {
        fun newInstance(courseId : String) : VerifyDialog{
            val dialog = VerifyDialog()
            val args = Bundle().apply {
                courseId?.let { putString("courseId", it) }
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val contentView = activity?.layoutInflater?.inflate(R.layout.fragment_verify_dialog, null) as View
        builder.setView(contentView)
        val dialog = builder.create()
        bluetoothManager = activity!!.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
        db = FirebaseFirestore.getInstance()
        if(arguments?.getString("courseId") != null){
            items = arguments?.getString("courseId").toString()
            Log.e("items", items)
        }

        bluetooth = contentView.findViewById(R.id.bluetooth)
        bluetoothProgress = contentView.findViewById(R.id.ble_progress)
        camera = contentView.findViewById(R.id.camera)
        cameraProgress = contentView.findViewById(R.id.camera_progress)
        onClickBluetoothButton()
        onClickCameraButton()
        return dialog
    }
    private fun onClickBluetoothButton(){
        bluetooth.setOnClickListener(View.OnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else{
                bluetooth.visibility = View.GONE
                bluetoothProgress.visibility = View.VISIBLE
                scanAvailableBluetooth()
            }
        })

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
            }
        }
    }

    private fun addUploadRecordToDb(uri: String){
        val db = FirebaseFirestore.getInstance()

        val data = HashMap<String, Any>()
        data["imageUrl"] = uri

        db.collection("posts")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(activity!!.applicationContext, "Saved to DB", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity!!.applicationContext, "Error saving to DB", Toast.LENGTH_LONG).show()
            }
    }

    private fun scanAvailableBluetooth(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        activity!!.applicationContext.registerReceiver(receiver, filter)
        isRegistered = true
        bluetoothAdapter.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    bluetoothLeScanner.startScan(bleScanner)

                }
            }
        }
    }

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            getServiceUUIDsList(result)
        }
    }

    private fun getServiceUUIDsList(scanResult: ScanResult?) {
        val parcelUUIDs = scanResult?.scanRecord?.serviceUuids
        if(parcelUUIDs != null){
            val serviceList = arrayListOf<UUID>()
            for(i in 0 until parcelUUIDs.size){
                val serviceUUID = parcelUUIDs[i].uuid
                Log.e("UUID", serviceUUID.toString())
                db.collection("courses").document(items).get().addOnSuccessListener { result ->
                    if(result.get("UUID") == serviceUUID.toString()){
                        bluetooth.setImageResource(R.drawable.ic_checked)
                        bluetooth.visibility = View.VISIBLE
                        bluetooth.isClickable = false
                        bluetoothProgress.visibility = View.GONE
                        bluetoothLeScanner.stopScan(bleScanner)
                        bluetoothAdapter.cancelDiscovery()
                        Log.e("Success", "SUCCESSS")
                    }
                }
            }
        }
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            return bluetoothAdapter.bluetoothLeScanner
        }

    override fun onStop() {
        Log.e("close dialog", "true")
        Log.e("ScanDeviceActivity", "onStop()")
        super.onStop()
        if(isRegistered){
            bluetoothLeScanner.stopScan(bleScanner)
            bluetoothAdapter.cancelDiscovery()
        }
    }
}
