package com.example.cs442katie

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
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
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var auth : FirebaseAuth
    lateinit var db : FirebaseFirestore
    lateinit var currentUser : User
    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("intent extras", intent.extras.toString())
        if(intent.extras != null) {
            Log.e("course", intent.extras!!.getString("course"))
        }
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        FirebaseMessaging.getInstance().subscribeToTopic("CS442")
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val activity_main = findViewById<RelativeLayout>(R.id.content_main)
        activity_main.visibility = View.GONE
        toolbar.visibility = View.GONE
        db.collection("users").document(auth.uid!!).get()
            .addOnSuccessListener { result ->
                currentUser = result.toObject(User::class.java) as User
                toolbar.title = result.get("fullName").toString()
                Log.e("user", currentUser.toString())

                val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
                val toggle = ActionBarDrawerToggle(this@MainActivity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
                drawerLayout.addDrawerListener(toggle)
                toggle.syncState()

                val requestQueue: RequestQueue by lazy {
                    Volley.newRequestQueue(this@MainActivity)
                }
                FirebaseMessaging.getInstance().subscribeToTopic("CS442")

                val userCourseList = result.get("course") as ArrayList<String>
                val coursesDatabase = FirebaseFirestore.getInstance().collection("courses").get()
                coursesDatabase.addOnSuccessListener { documents ->
                    val courseList = documents.filter {
                        it.id in userCourseList
                    }.map {
                        it.toObject(Course::class.java)
                    }
                    Log.e("courses", courseList.toString())
                    val courseMainAdapter =
                        CourseMainAdapter(this@MainActivity, courseList, View.OnClickListener {
                            val intent = Intent(this@MainActivity, CourseActivity::class.java)
                            intent.putExtra("courseId", findViewById<TextView>(R.id.course_id)?.text)
                            intent.putExtra("courseName", findViewById<TextView>(R.id.course_name)?.text)
                            startActivity(intent)
                        }, View.OnClickListener {
                            val notification = JSONObject()
                            val notificationData = JSONObject()

                            try {
                                notification.put("to", "/topics/CS442")
                                notificationData.put("title", findViewById<TextView>(R.id.course_name).text)
                                notificationData.put("message", "Class is checking attendance")
                                notificationData.put("courseId", findViewById<TextView>(R.id.course_id).text)
                                notification.put("data", notificationData)
                                Log.e("notification", notification.toString(2))
                            } catch (e: JSONException) {
                                Log.e("TAG", "onCreate: " + e.message)
                            }
                            sendNotification(requestQueue, notification)

                        }, auth.currentUser!!.uid)
                    val recycler = findViewById<RecyclerView>(R.id.course_lists)
                    recycler.setHasFixedSize(true)

                    recycler.layoutManager = LinearLayoutManager(this@MainActivity)
                    recycler.adapter = courseMainAdapter
                    activity_main.visibility = View.VISIBLE
                    toolbar.visibility = View.VISIBLE
                    setSupportActionBar(toolbar)
                    }
                }
    }

    private fun sendNotification(requestQueue: RequestQueue, notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                // Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(applicationContext, "Request error", Toast.LENGTH_LONG).show()
                Log.e("TAG", "onErrorResponse: Didn't work")
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_settings) {
            Log.d("sign out", "Signed out")
            auth.signOut()
            val intent = Intent(this@MainActivity, StartActivity :: class.java)
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
}
