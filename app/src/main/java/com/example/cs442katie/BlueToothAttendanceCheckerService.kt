package com.example.cs442katie

import android.app.Activity
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.FileDescriptor
import java.util.*
import kotlin.collections.ArrayList
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name




private const val TAG = "Service Bluetooth"

class BlueToothAttendanceCheckerService : Service() {

    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothScanner: BluetoothLeScanner
    lateinit var db : FirebaseFirestore
    private val binder = LocalBinder()

    var attendanceCode: String? = null
    var codeHost : String? = null;
    var courseIdHost : String? = null
    var studentId : String? = null
    var attendanceChecked = false

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        db = FirebaseFirestore.getInstance()
    }
    override fun onBind(intent: Intent): IBinder {
        attendanceCode = intent.getStringExtra("attendanceCode")
        courseIdHost = intent.getStringExtra("courseId")
        studentId = intent.getStringExtra("studentId")
        Log.e("courseId", courseIdHost)
        Log.e("studentId", studentId)
        return LocalBinder()
    }

    //FOR HOST
    private val advertiseCallback = object : AdvertiseCallback() {

        override fun onStartSuccess(settingsInEffect : AdvertiseSettings) {
            Log.e(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "LE Advertise Failed: $errorCode")
        }
    }

    fun startAdvertising(attendanceCode : String?) {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser

        val pUUID = ParcelUuid(UUID.fromString(attendanceCode))

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(pUUID)
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.e(TAG, "Failed to create advertiser")

        db.collection("courses").document(courseIdHost!!).update("UUID", attendanceCode);
    }

    fun stopAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.let {
            it.stopAdvertising(advertiseCallback)
        } ?: Log.e(TAG, "Failed to create advertiser")
    }

    //FOR STUDENT

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    bluetoothScanner.startScan(bleScanner)
                }
            }
        }
    }

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            getServiceUUIDsList(result, courseIdHost)
        }
    }

    private fun scanAvailableBluetooth(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        bluetoothAdapter.startDiscovery()
    }

    fun getServiceUUIDsList(scanResult: ScanResult?, courseId: String?) {
        val parcelUUIDs = scanResult?.scanRecord?.serviceUuids
        if(parcelUUIDs != null){
            val serviceList = arrayListOf<UUID>()
            for(i in 0 until parcelUUIDs.size){
                val serviceUUID = parcelUUIDs[i].uuid
                Log.e("UUID", serviceUUID.toString())
                db.collection("courses").document(courseId!!).get().addOnSuccessListener { result ->
                    if(result.get("UUID") == serviceUUID.toString()){
                        codeHost = serviceUUID.toString()
                        startAdvertising(codeHost)
                        val lectureArr = result.get("lecture") as ArrayList<String>
                        var lastElem = lectureArr[lectureArr.size - 1]
                        val newDb = FirebaseFirestore.getInstance().collection("courses").document(courseId!!)
                        newDb.update("lecture", FieldValue.arrayRemove(lastElem))
                        newDb.update("lecture", FieldValue.arrayUnion(lastElem + "-" + studentId))
                        attendanceChecked = true
                        sendMessage()
                        bluetoothScanner.stopScan(bleScanner)
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val intent = Intent("attendanceChecked")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    inner class LocalBinder : Binder(){
        fun getService(): BlueToothAttendanceCheckerService {
            return this@BlueToothAttendanceCheckerService
        }

    }

}
