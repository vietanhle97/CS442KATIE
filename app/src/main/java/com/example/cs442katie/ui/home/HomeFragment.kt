package com.example.cs442katie.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.cs442katie.*
import com.example.cs442katie.ui.gallery.GalleryFragment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.course_main.*
import org.json.JSONException
import org.json.JSONObject

class HomeFragment : Fragment() {
    private val FCM_API = "https://fcm.googleapis.com/fcm/send"

    override fun onCreateView( inflater: LayoutInflater,  container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val usersDatabase = (activity as MainActivity).db.collection("users").document((activity as MainActivity).auth.uid as String)

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val requestQueue: RequestQueue by lazy {
            Volley.newRequestQueue(root.context)
        }
        FirebaseMessaging.getInstance().subscribeToTopic("CS442")

        usersDatabase.get().addOnSuccessListener { result ->
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
                    CourseMainAdapter(root.context, courseList, View.OnClickListener {
                        val intent = Intent(root.context, CourseActivity::class.java)
                        intent.putExtra("courseId", view?.findViewById<TextView>(R.id.course_id)?.text)
                        intent.putExtra("courseName", view?.findViewById<TextView>(R.id.course_name)?.text)
                        startActivity(intent)
                    }, View.OnClickListener {
                        val notification = JSONObject()
                        val notificationData = JSONObject()

                        try {
                            notification.put("to", "/topics/CS442")
                            notificationData.put("title", (view?.parent as View).findViewById<TextView>(R.id.course_name).text)
                            notificationData.put("message", "Class is checking attendance")
                            notificationData.put("courseId", (view?.parent as View).findViewById<TextView>(R.id.course_id).text)
                            notification.put("data", notificationData)
                            Log.e("notification", notification.toString(2))
                        } catch (e: JSONException) {
                            Log.e("TAG", "onCreate: " + e.message)
                        }
                        sendNotification(requestQueue, notification)

                    }, (activity as MainActivity).auth.currentUser!!.uid)
                val recycler = root.findViewById<RecyclerView>(R.id.course_lists)
                recycler.setHasFixedSize(true)

                recycler.layoutManager = LinearLayoutManager(root.context)
                recycler.adapter = courseMainAdapter
            }
        }

        return root

    }

    private fun sendNotification(requestQueue: RequestQueue, notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                // Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText((activity as MainActivity).applicationContext, "Request error", Toast.LENGTH_LONG).show()
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

}