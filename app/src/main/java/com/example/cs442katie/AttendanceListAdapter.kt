package com.example.cs442katie

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.opencensus.resource.Resource
import org.w3c.dom.Text

class AttendanceListAdapter(
    val context: Context,
    val userList: List<User>,
    val courseId: String,
    val today_attendance: HashMap<String, Boolean>): RecyclerView.Adapter<AttendanceListAdapter.CourseMainViewHolder> () {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseMainViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.attendance_list, parent, false)
        return CourseMainViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: CourseMainViewHolder, position: Int) {
        val user = userList[position]
        holder.full_name.text = user.fullName
        holder.count_attendance.text = user.course[courseId].toString()
        if(today_attendance[user.id]!!){
            holder.today_attendance.text = "Yes"
            holder.today_attendance.setTextColor(Color.GREEN)
        } else{
            holder.today_attendance.text = "No"
            holder.today_attendance.setTextColor(Color.RED)
        }

    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val full_name = view.findViewById<TextView>(R.id.full_name)
        var today_attendance = view.findViewById<TextView>(R.id.today_attendance)
        val count_attendance =view.findViewById<TextView>(R.id.count_attendance)


    }
}