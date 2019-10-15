package com.example.cs442katie

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CourseMainAdapter(private var context: Context, private var courseMainList: ArrayList<CourseMain>, private var courseListener: View.OnClickListener ,private var attendanceListener : View.OnClickListener) : RecyclerView.Adapter<CourseMainAdapter.CourseMainViewHolder> (){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseMainViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.course_main, parent, false)
        return CourseMainViewHolder(view)
    }

    override fun getItemCount(): Int {
        return courseMainList.size
    }

    override fun onBindViewHolder(holder: CourseMainViewHolder, position: Int) {
        val courseMain = courseMainList[position]
        holder.courseName.text = courseMain.getCourseName()
        holder.callAttendanceButton.setOnClickListener(attendanceListener)
        holder.course_main.setOnClickListener(courseListener)
    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val course_main = view.findViewById<CardView>(R.id.course_main)
        val courseName = view.findViewById<TextView>(R.id.course_name)
        val callAttendanceButton = view.findViewById<Button>(R.id.call_attendance_button)

    }
}