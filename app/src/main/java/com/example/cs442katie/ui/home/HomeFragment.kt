package com.example.cs442katie.ui.home

import android.os.Bundle
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
import com.example.cs442katie.CourseMain
import com.example.cs442katie.CourseMainAdapter
import com.example.cs442katie.R
import com.example.cs442katie.ui.gallery.GalleryFragment

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView( inflater: LayoutInflater,  container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(this, Observer {
            textView.text = it
        })

        val courseMainList = arrayListOf<CourseMain>()
        val courseMainListName = arrayListOf("Mobile Computing", "HCI", "OS", "Service Computing")
        for(i in 0..3){
            courseMainList.add(CourseMain(courseMainListName[i]))
        }

        val courseMainAdapter = CourseMainAdapter(root.context, courseMainList, View.OnClickListener {
            val fragment = GalleryFragment()
            val transaction = activity!!.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.home_fragment_container, fragment).addToBackStack("homeFragment").commit()
        }, View.OnClickListener {
            val message = Toast.makeText(root.context, "Call Attendance", Toast.LENGTH_SHORT)
            message.show()
        })
        val recycler = root.findViewById<RecyclerView>(R.id.course_lists)
        recycler.setHasFixedSize(true)

        recycler.layoutManager = LinearLayoutManager(root.context)
        recycler.adapter = courseMainAdapter
        return root

    }
}