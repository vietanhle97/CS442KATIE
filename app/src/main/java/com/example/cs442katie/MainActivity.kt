package com.example.cs442katie

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.cs442katie.databinding.ActivityMainBinding
import com.example.cs442katie.databinding.NavHeaderMainBinding
import com.example.cs442katie.models.MyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.attendance_list.*
import kotlinx.android.synthetic.main.content_main.view.*
import kotlinx.android.synthetic.main.course_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.random.Random

private const val TAG = "KATIE"


class MainActivity : AppCompatActivity() {
    private var MY_UUID = UUID.randomUUID()
    lateinit var mainBinding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val REQUEST_ENABLE_BT = 20
    private val REQUEST_DISCOVERABLE_BL = 30
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var auth : FirebaseAuth
    lateinit var db : FirebaseFirestore
    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private var isBroadcastReceiverRegistered = false
    lateinit var currentCourse : Course
    lateinit var serviceIntent : Intent
    var serviceIsBound = false;

    private val serviceConnection = object : ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
            Log.e("service is Disconnected", "TRUE")
        }
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as BlueToothAttendanceCheckerService.LocalBinder
            val blueToothAttendanceCheckerService = binder.getService()
            serviceIsBound = true;
            blueToothAttendanceCheckerService.startAdvertising(MY_UUID.toString())
            loopChecking()
        }
    }
    companion object {
        var user: User = User()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if(bluetoothAdapter == null){
            finish()
        }
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        FirebaseMessaging.getInstance().subscribeToTopic("CS442")
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(mainBinding.drawerLayout.findViewById(R.id.toolbar))
        mainScreenSetup()
    }

    private fun mainScreenSetup(){
        val activityMain = findViewById<RelativeLayout>(R.id.content_main)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = mainBinding.navView
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                R.id.nav_tools), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val toggle = ActionBarDrawerToggle(this@MainActivity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        activityMain.visibility = View.GONE
        toolbar.visibility = View.GONE
        serviceIntent = Intent(this@MainActivity, BlueToothAttendanceCheckerService::class.java)

        val model = ViewModelProviders.of(this).get(MyViewModel::class.java)
        model.getCurrentUser().observe(this, Observer { currentUser ->
            toolbar.title = currentUser.fullName
            val faceRef = FirebaseStorage.getInstance().reference.child(currentUser.faceUri)
            navView.getHeaderView(0).apply {
                user_name.text = currentUser.fullName
                faceRef.downloadUrl.addOnSuccessListener {
                    Glide.with(this).load(it).into(main_user_avatar)
                }
            }
            FirebaseMessaging.getInstance().subscribeToTopic("CS442")

            model.getCourseList(currentUser).observe(this, Observer {courseList ->
                val courseMainAdapter = CourseMainAdapter(this@MainActivity, serviceIntent, courseList, {course : Course -> onCourseMainItemClick(course)}
                    , {course : Course -> onCallAttendanceButtonClick(course)}, auth.currentUser!!.uid, serviceConnection)
                val recycler = mainBinding.drawerLayout.findViewById<RecyclerView>(R.id.course_lists)
                recycler.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(this@MainActivity)
                    adapter = courseMainAdapter
                }
                activityMain.visibility = View.VISIBLE
                toolbar.visibility = View.VISIBLE
            })

        })
    }

    fun loopChecking(){
        Timer("schedule", true).schedule(60000) {
            var stop = false;
            FirebaseFirestore.getInstance().collection("isCheckingAttendance").document(currentCourse.courseId).
                get().addOnSuccessListener {
                val checkingAttendance = it.get("isCheckingAttendance")
                if(checkingAttendance == false)
                    stop = true;
            }
//            Log.e("stop", stop.toString())
//            Log.e("serviceIsBound", serviceIsBound.toString())
            if(!stop){
                MY_UUID = UUID.randomUUID()
                serviceIntent.putExtra("attendanceCode", MY_UUID.toString())
                Log.e("UUID", MY_UUID.toString())
                if(serviceIsBound){
                    unbindService(serviceConnection)
                    stopService(serviceIntent)
                    this@MainActivity.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                } else{
                    stopService(serviceIntent)
                }
            }
        }
    }

    private fun sendNotification(requestQueue: RequestQueue, notification: JSONObject) {
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                // Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(applicationContext, "Request error", Toast.LENGTH_LONG).show()
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = "key=AAAAgnRkfDA:APA91bHDoHPaekd3AQsdXP79Uq_c9ZOBE-IEtFlRWwSuUo0gOY9BhaO-iIPd5q7sQ-SYNTgfqH_aPB9bXgzuvqGxxC1805VmVJvQFP8FT0LN-3BNBALPNcMoNSuvnl2DDPZPy1n8O-9A"
                params["Content-Type"] = "application/json"
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun onCourseMainItemClick(course: Course){
        val intent = Intent(this@MainActivity, CourseActivity::class.java)
        intent.putExtra("studentId", auth.currentUser!!.uid)
        intent.putExtra("courseId", course.courseId)
        intent.putExtra("courseName", course.courseName)
        intent.putExtra("adminId", course.admin)
        startActivity(intent)
    }

    private fun onCallAttendanceButtonClick(course: Course){
        currentCourse = course
        serviceIntent.putExtra("courseId", course.courseId)
        serviceIntent.putExtra("studentId", auth.currentUser?.uid)
        serviceIntent.putExtra("attendanceCode", MY_UUID.toString())
        serviceIntent.putExtra("hostId", course.admin)
        if(bluetoothAdapter.isEnabled){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else{
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
                putExtra("Hello", "Viet Anh")

            }
            setResult(60, discoverableIntent)
            startActivityForResult(intent, REQUEST_DISCOVERABLE_BL)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
//        Log.e("data", intent?.data.toString())
//        Log.e("requestCode", requestCode.toString())
//        Log.e("haha", RESULT_OK.toString())
        if(requestCode == REQUEST_ENABLE_BT){
//            Log.e("resultCode", resultCode.toString())
            if(resultCode != Activity.RESULT_OK){
                Toast.makeText(this, "Please turn on bluetooth to check attendance", Toast.LENGTH_SHORT).show()
            } else {
                bluetoothAdapter.enable()
                val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
                }
                discoverableIntent.putExtra("Hello", "Viet Anh")
                setResult(60, discoverableIntent)
                startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BL)
            }
        } else if(requestCode == REQUEST_DISCOVERABLE_BL) {
//            Log.e("resultCode", resultCode.toString())

            if(resultCode != 60) {
                Toast.makeText(
                    this,
                    "Please turn on bluetooth for 60 secs to check attendance",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                FirebaseFirestore.getInstance().collection("courses").document(currentCourse.courseId).update("lecture", FieldValue.delete())
                FirebaseFirestore.getInstance().collection("courses").document(currentCourse.courseId).get().addOnSuccessListener {
                    val studentList = it.get("student") as ArrayList<String>
                    Log.e("studentList", studentList.size.toString())
                    for (i in studentList){
                        FirebaseFirestore.getInstance().collection("users").document(i).get().addOnSuccessListener { result ->
                            Log.e("studentID", i)
                            Log.e("true", result.contains("currentClassCount").toString())
                            FirebaseFirestore.getInstance().collection("users").document(i).update("course.${currentCourse.courseId}", 0)
                            FirebaseFirestore.getInstance().collection("users").document(i).update("currentClassCount.${currentCourse.courseId}", 0)

                        }
                    }
                }
                this.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                val requestQueue: RequestQueue by lazy {
                    Volley.newRequestQueue(this@MainActivity)
                }
                val notification = JSONObject()
                val notificationData = JSONObject()
                try {
                    notification.put("to", "/topics/CS442")
                    notificationData.put("title", currentCourse.courseName)
                    notificationData.put("message", "Class is checking attendance")
                    notificationData.put("studentId", user.studentId)
                    notificationData.put("courseId", currentCourse.courseId)
                    notificationData.put("adminId", currentCourse.admin)
                    notification.put("data", notificationData)
//                    Log.e("notification", notification.toString(2))
                } catch (e: JSONException) {
//                    Log.e("TAG", "onCreate: " + e.message)
                }
                sendNotification(requestQueue, notification)
                db.collection("isCheckingAttendance").document(currentCourse.courseId).update("isCheckingAttendance", true)

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_settings) {
//            Log.d("sign out", "Signed out")
            auth.signOut()
            val intent = Intent(this@MainActivity, StartActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        supportFragmentManager.popBackStack()

    }

    override fun onNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onDestroy() {
        super.onDestroy()
        if(serviceIsBound){
            unbindService(serviceConnection)
            serviceIsBound = false
        }
        if(bluetoothAdapter.isEnabled){
            bluetoothAdapter.disable()
//            Log.e("disable", "disabled")
        }
    }

    override fun onResume() {
//        Log.e("unregister", isBroadcastReceiverRegistered.toString())
        super.onResume()
        if(!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
        }

    }

}
