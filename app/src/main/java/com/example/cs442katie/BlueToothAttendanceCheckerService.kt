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
    var advertising = false;
    var scanning = false;
    var prevUUID = ""
    private val binder = LocalBinder()

    var attendanceCode: String? = null
    var codeHost : String? = null
    var hostId : String? = null
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
        hostId = intent.getStringExtra("hostId")

        return LocalBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        if(advertising)
            stopAdvertising()
        advertising = false;
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
        var stop = false;
        FirebaseFirestore.getInstance().collection("courses").document(courseIdHost!!).
            get().addOnSuccessListener {
            val checkingAttendance = it.get("isCheckingAttendance")
            if(checkingAttendance == false)
                stop = true;
        }
        if(stop)
            return;
        advertising = true;
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
                val lectureArr = it.get("lecture") as ArrayList<HashMap<String, Long> >
                val year = Calendar.getInstance().get(Calendar.YEAR).toLong()
                val month = Calendar.getInstance().get(Calendar.MONTH).toLong()
                val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toLong()
                Log.e("lecArr",lectureArr.size.toString())
                if(lectureArr.size == 0){
                    lectureArr.add(hashMapOf("Year" to year, "Month" to month,
                        "Day" to day, "Check_Count" to 1L))
                    Log.e("11","11")
                }
                else if(studentId == hostId){
                    var lastLecture = lectureArr[lectureArr.size - 1]
                    if(lastLecture["Day"]?.toLong() == day && lastLecture["Month"]?.toLong() == month && lastLecture["Year"]?.toLong() == year){
                        lectureArr[lectureArr.size - 1]["Check_Count"] = lectureArr[lectureArr.size - 1]["Check_Count"]!! + 1
                        Log.e("check_count","did it")
                    } else {
                        lectureArr.add(hashMapOf("Year" to year, "Month" to month,
                            "Day" to day, "Check_Count" to 1L))
                        Log.e("11","12")
                    }
                }
                Log.e("asad", studentId + " " + hostId)
                newDb.update("lecture", lectureArr)
            }
        } ?: Log.e(TAG, "Failed to create advertiser")


    }

    fun stopAdvertising() {
        advertising = false;
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.let {
            it.stopAdvertising(advertiseCallback)
        } ?: Log.e(TAG, "Failed to create advertiser")
    }

    //FOR STUDENT
    fun loopChecking(){
        Timer("schedule", true).schedule(10000) {
            FirebaseFirestore.getInstance().collection("courses").document(courseIdHost!!).
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
        loopChecking()
    }

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            getServiceUUIDsList(result, courseIdHost)
        }
    }

    fun getServiceUUIDsList(scanResult: ScanResult?, courseId: String?) {
        db.collection("courses").document(courseId!!).get().addOnSuccessListener { result ->
            val parcelUUIDs = scanResult?.scanRecord?.serviceUuids
            val lectureArr = result.get("lecture") as ArrayList<HashMap<String, Long>>
            val isCheckingAttendance = result.get("isCheckingAttendance") as Boolean
            val currentLecture = lectureArr[lectureArr.size - 1]
            if(parcelUUIDs != null){
                val hostUUID = result.get("UUID").toString()
                for (i in parcelUUIDs) {
                    Log.e("UUID", i.uuid.toString())
                    if (result.get("UUID") == i.uuid.toString() && isCheckingAttendance) {
                        if (advertising) stopAdvertising()

                        if(studentId!! in currentLecture.keys){
                            attendanceChecked  = true
                        }
                        val newDb = FirebaseFirestore.getInstance().collection("courses")
                            .document(courseId!!)

                        if(!attendanceChecked){
                            lectureArr[lectureArr.size-1][studentId!!] = 1
                            newDb.update("lecture", lectureArr)
                        } else{
                            lectureArr[lectureArr.size - 1][studentId!!] = lectureArr[lectureArr.size - 1][studentId!!] as Long  + 1
                            newDb.update("lecture", lectureArr)
                        }
                        sendMessage()
                        startAdvertising(hostUUID)
                        attendanceChecked = false
                        break
    //                        if (!attendanceChecked) {
    //                            val newDb = FirebaseFirestore.getInstance().collection("courses")
    //                                .document(courseId!!)
    //                            if (lectureArr[lectureArr.size - 1][studentId!!] == null) {
    //                                lectureArr[lectureArr.size - 1][studentId!!] =
    //                                    lectureArr[lectureArr.size - 1]["Check_Count"]!!
    //                                newDb.update("lecture", lectureArr)
    //
    //                                FirebaseFirestore.getInstance().collection("users")
    //                                    .document(studentId!!).get()
    //                                    .addOnSuccessListener { result ->
    //                                        Log.e("studentId", studentId)
    //                                        val map = result.get("course") as HashMap<String, Long>
    //                                        val cnt = 1
    //                                        FirebaseFirestore.getInstance().collection("users")
    //                                            .document(studentId!!).update(
    //                                                mapOf("course.$courseId" to cnt)
    //                                            )
    //                                    }
    //
    //                            } else if (lectureArr[lectureArr.size - 1][studentId!!] != lectureArr[lectureArr.size - 1]["Check_Count"]!!) {
    //                                lectureArr[lectureArr.size - 1][studentId!!] =
    //                                    lectureArr[lectureArr.size - 1]["Check_Count"]!!
    //                                newDb.update("lecture", lectureArr)
    //
    //                            }
    //                        }
    //                        attendanceChecked = false
    //                        if (scanning)
    //                            bluetoothScanner.stopScan(bleScanner)
    //                        scanning = false;
                    } else {
                        bluetoothScanner.stopScan(bleScanner)
                        stopAdvertising()
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
