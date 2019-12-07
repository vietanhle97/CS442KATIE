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
import com.google.firebase.firestore.FirebaseFirestore
import io.opencensus.resource.Resource
import kotlinx.android.synthetic.main.course_main.*

class CourseMainAdapter(
    val context: Context,
    val courseList: List<Course>,
    val courseListener: (Course) -> Unit,
    val attendanceListener : (Course) -> Unit,
    val uid : String): RecyclerView.Adapter<CourseMainAdapter.CourseMainViewHolder> () {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseMainViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.course_main, parent, false)
        return CourseMainViewHolder(view)
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    override fun onBindViewHolder(holder: CourseMainViewHolder, position: Int) {
        val course = courseList[position]
        if(course.admin != uid) {
            holder.callAttendanceButton.visibility = View.GONE
            holder.admin.visibility = View.GONE
            holder.course.setCardBackgroundColor(ContextCompat.getColor(context, R.color.member))
        }

        holder.bind(course, courseListener, attendanceListener)
    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val course = view.findViewById<CardView>(R.id.course)
        val courseName = view.findViewById<TextView>(R.id.course_name)
        val callAttendanceButton = view.findViewById<Button>(R.id.call_attendance_button)
        val courseId =  view.findViewById<TextView>(R.id.course_id)
        val courseInstructor = view.findViewById<TextView>(R.id.course_instructor)
        val admin = view.findViewById<TextView>(R.id.admin)

        fun bind(courseMain: Course, courseListener: (Course) -> Unit, attendanceListener: (Course) -> Unit){
            courseName.text = courseMain.courseName
            courseId.text = courseMain.courseId
            courseInstructor.text = courseMain.instructor
//            callAttendanceButton.setOnClickListener(attendanceListener)
            course.setOnClickListener(View.OnClickListener {
                courseListener(courseMain)
            })

            callAttendanceButton.setOnClickListener(View.OnClickListener {

                if(callAttendanceButton.text != "STOP CALLING"){
                    attendanceListener(courseMain)
                    callAttendanceButton.text = "STOP CALLING"
                    callAttendanceButton.setBackgroundColor(Color.BLACK)
                    callAttendanceButton.setTextColor(Color.WHITE)
                } else {
                    FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).update("isCheckingAttendance", false)
                    callAttendanceButton.text = "CALL ATTENDANCE"
                    callAttendanceButton.setBackgroundColor(Color.WHITE)
                    callAttendanceButton.setTextColor(Color.BLACK)
                }
            })
        }
    }
}