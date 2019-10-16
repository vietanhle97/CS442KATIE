package com.example.cs442katie

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CourseMainAdapter(
        private var context: Context, private var courseList: List<Course>,
        private var courseListener: View.OnClickListener,
        private var attendanceListener : View.OnClickListener,
        private var uid : String) : RecyclerView.Adapter<CourseMainAdapter.CourseMainViewHolder> () {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseMainViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.course_main, parent, false)
        return CourseMainViewHolder(view)
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    override fun onBindViewHolder(holder: CourseMainViewHolder, position: Int) {
        val course = courseList[position]
        holder.courseName.text = course.courseName
        holder.courseId.text = course.courseId
        holder.courseInstructor.text = course.instructor
            holder.callAttendanceButton.setOnClickListener(attendanceListener)
        holder.course.setOnClickListener(courseListener)
        if(course.admin != uid)
            holder.callAttendanceButton.visibility = View.GONE

    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val course = view.findViewById<CardView>(R.id.course)
        val courseName = view.findViewById<TextView>(R.id.course_name)
        val callAttendanceButton = view.findViewById<Button>(R.id.call_attendance_button)
        val courseId =  view.findViewById<TextView>(R.id.course_id)
        val courseInstructor = view.findViewById<TextView>(R.id.course_instructor)
    }


}