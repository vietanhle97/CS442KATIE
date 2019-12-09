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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.vision.text.Line
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.opencensus.resource.Resource
import org.w3c.dom.Text

class AttendanceListAdapter(
    val context: Context,
    val userList: List<User>,
    val courseId: String,
    val today_attendance: HashMap<String, Boolean>,
    val isAdmin : Boolean): RecyclerView.Adapter<AttendanceListAdapter.CourseMainViewHolder> () {



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
        holder.student_id.text = "ID: ${user.studentId}"
        holder.count_attendance.text = user.course[courseId].toString()
        if(today_attendance[user.id] != null && today_attendance[user.id]!!){
            holder.today_attendance.setImageResource(R.drawable.ic_checked)
        } else{
            holder.today_attendance.setImageResource(R.drawable.ic_cancel)
        }
        val faceRef = FirebaseStorage.getInstance().reference.child(user.faceUri)
        faceRef.downloadUrl.addOnSuccessListener {
            Glide.with(context)
                .load(it)
                .into(holder.user_avatar)

        }
        if (!isAdmin){
            holder.today_attendance_holder.visibility = View.INVISIBLE
            holder.count_attendance_holder.visibility = View.INVISIBLE
            holder.student_id.visibility = View.INVISIBLE
        }

    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val full_name = view.findViewById<TextView>(R.id.full_name)
        val student_id = view.findViewById<TextView>(R.id.student_id)
        var today_attendance = view.findViewById<ImageView>(R.id.today_attendance)
        val count_attendance =view.findViewById<TextView>(R.id.count_attendance)
        val user_avatar = view.findViewById<ImageView>(R.id.user_avatar)
        val today_attendance_holder = view.findViewById<LinearLayout>(R.id.today_attendance_holder)
        val count_attendance_holder = view.findViewById<LinearLayout>(R.id.count_attendance_holder)

    }
}