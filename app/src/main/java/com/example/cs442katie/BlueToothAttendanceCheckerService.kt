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
import java.text.SimpleDateFormat
import kotlin.collections.HashMap
import kotlin.concurrent.schedule


private const val TAG = "Service Bluetooth"

class BlueToothAttendanceCheckerService : Service() {

    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothScanner: BluetoothLeScanner
    lateinit var db : FirebaseFirestore
    var advertising = false
    var scanning = false
    var attendanceCode: String? = null
    var codeHost : String? = null
    var hostId : String? = null
    var courseIdHost : String? = null
    var studentId : String? = null

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
        hostId = intent.getStringExtra("hostId")

        return LocalBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        if(advertising)
            stopAdvertising()
        advertising = false
        return true
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
        var stop = false
        FirebaseFirestore.getInstance().collection("isCheckingAttendance").document(courseIdHost!!).
            get().addOnSuccessListener {
            val checkingAttendance = it.get("isCheckingAttendance")
            if(checkingAttendance == false)
                stop = true
        }
        if(stop)
            return
        advertising = true
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser
        val MY_UUID = UUID.randomUUID()
        val pUUID = ParcelUuid(MY_UUID)

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
            db.collection("courses").document(courseIdHost!!).update("UUID", MY_UUID.toString())
            Log.e("asda","advertising")
            val newDb = FirebaseFirestore.getInstance().collection("courses").document(courseIdHost!!)
            newDb.get().addOnSuccessListener {
                var lectureArr = it.get("lecture") as HashMap<String, Long>?
                val year = Calendar.getInstance().get(Calendar.YEAR).toLong()
                val month = Calendar.getInstance().get(Calendar.MONTH).toLong()
                val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toLong()
                if(lectureArr == null){
                    newDb.update("lecture.Year" , year)
                    newDb.update("lecture.Month" , month)
                    newDb.update("lecture.Day" , day)
                    newDb.update("lecture.Check_Count" , 1L)
                }
                else if(studentId == hostId) {
                    if(lectureArr["Day"]?.toLong() == day && lectureArr["Month"]?.toLong() == month && lectureArr["Year"]?.toLong() == year){
                        newDb.update("lecture.Check_Count" , FieldValue.increment(1))
                    } else {
                        newDb.update("lecture.Year" , year)
                        newDb.update("lecture.Month" , month)
                        newDb.update("lecture.Day" , day)
                        newDb.update("lecture.Check_Count" , 1L)
                    }
                }
            }
        } ?: Log.e(TAG, "Failed to create advertiser")


    }

    fun stopAdvertising() {
        advertising = false
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.let {
            it.stopAdvertising(advertiseCallback)
        } ?: Log.e(TAG, "Failed to create advertiser")
    }

    //FOR STUDENT
    fun loopChecking(){
        Timer("schedule", true).schedule(15000) {
            FirebaseFirestore.getInstance().collection("isCheckingAttendance").document(courseIdHost!!).
                get().addOnSuccessListener {
                val checkingAttendance = it.get("isCheckingAttendance")
                if(checkingAttendance == false)
                    bluetoothScanner.stopScan(bleScanner)
                else {
                    startScan()
                }
            }

        }
    }

    fun startScan(){
        bluetoothScanner.startScan(bleScanner)
        scanning = true
        loopChecking()
    }

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            getServiceUUIDsList(result, courseIdHost)
        }
    }

    fun getServiceUUIDsList(scanResult: ScanResult?, courseId: String?) {
        val parcelUUIDs = scanResult?.scanRecord?.serviceUuids
        if(parcelUUIDs != null){
            val serviceList = arrayListOf<UUID>()
            Log.e("asdasd","callback " + parcelUUIDs.size.toString())
            for(i in 0 until parcelUUIDs.size) {
                val serviceUUID = parcelUUIDs[i].uuid
                Log.e("UUID", serviceUUID.toString())
                db.collection("courses").document(courseId!!).get().addOnSuccessListener { result ->

                    Log.e("found",serviceUUID.toString())
                    if(result.get("UUID") == serviceUUID.toString()){
                        codeHost = serviceUUID.toString()
                        if(advertising)
                            stopAdvertising()
                        startAdvertising(codeHost)
                        val lectureArr = result.get("lecture") as HashMap<String, Long>

                        db.collection("courses").document(courseId).update("lecture.${studentId!!}" , lectureArr["Check_Count"]).addOnSuccessListener {
                            Log.e("update", "success")
//                            Log.e("Start sleeping", "123")
//                            Thread.sleep(5000)
//                            Log.e("Done sleeping", "123")
                        }.addOnFailureListener {
                            Log.e("update", it.message)
                        }

                        if(lectureArr[studentId!!] == null) {
                            FirebaseFirestore.getInstance().collection("users").document(studentId!!).update("currentClassCount.$courseId", 1)
                        }

                        else if(lectureArr[studentId!!] != lectureArr["Check_Count"]!!){
                            FirebaseFirestore.getInstance().collection("users").document(studentId!!).
                                    update("currentClassCount.$courseId", FieldValue.increment(1))
                        }
                        sendMessage()

                        if(scanning)
                            bluetoothScanner.stopScan(bleScanner)
                        scanning = false
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
