package com.example.cs442katie.ui.home

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
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/CS442")

        usersDatabase.get().addOnSuccessListener { result ->
            val userCourseList = result.get("course") as ArrayList<String>
            val coursesDatabase = FirebaseFirestore.getInstance().collection("courses")
            val courseTasks = userCourseList.map { coursesDatabase.document("${it!!}").get() }
            Tasks.whenAllSuccess<DocumentSnapshot>(courseTasks).addOnSuccessListener { documents ->
                val courseList = documents.map { it.toObject(Course :: class.java) } as List<Course>
                val courseMainAdapter = CourseMainAdapter(root.context, courseList, View.OnClickListener {
                    val fragment = GalleryFragment()
                    val transaction = activity!!.supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.home_fragment_container, fragment).addToBackStack("homeFragment").commit()
                }, View.OnClickListener {
                    val notification = JSONObject()
                    val notificationBody = JSONObject()

                    try {
                        notification.put("to", "/topics/CS442")
                        notificationBody.put("title", "Enter_title")
                        notificationBody.put("message", "vdx")
                        notification.put("data", notificationBody)
//                    notification.put("data", notifcationBody)
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
        val jsonObjectRequest = object : JsonObjectRequest(Method.POST, FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText((activity as MainActivity).applicationContext, "Request error", Toast.LENGTH_LONG).show()
                Log.e("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = "key=AAAAKM1G8VU:APA91bF0BgkxTiymt9S2G49s9AAN6CsMt-DpUB00uXvmzY88gaAKcChPD0wQOze2tbZlJ4brDmmiP0Z5WrpqP40QwD3mCLJBIvU8flrGfDLcbdLSuKURdihsN_N71CzgmMU2GX7AAfuj"
                params["Content-Type"] = "application/json"
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

}