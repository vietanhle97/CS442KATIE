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
import com.example.cs442katie.*
import com.example.cs442katie.ui.gallery.GalleryFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView( inflater: LayoutInflater,  container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val coursesDatabase = (activity as MainActivity).db.collection("courses")

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)

        coursesDatabase.get().addOnSuccessListener { result ->
            val courseList = result.map {
                it.toObject(Course :: class.java)
            }
            val courseMainAdapter = CourseMainAdapter(root.context, courseList, View.OnClickListener {
                val fragment = GalleryFragment()
                val transaction = activity!!.supportFragmentManager.beginTransaction()
                transaction.replace(R.id.home_fragment_container, fragment).addToBackStack("homeFragment").commit()
            }, View.OnClickListener {
                val message = Toast.makeText(root.context, "Call Attendance", Toast.LENGTH_SHORT)
                message.show()
            }, (activity as MainActivity).auth.currentUser!!.uid)
            val recycler = root.findViewById<RecyclerView>(R.id.course_lists)
            recycler.setHasFixedSize(true)

            recycler.layoutManager = LinearLayoutManager(root.context)
            recycler.adapter = courseMainAdapter
        }

        return root

    }
}