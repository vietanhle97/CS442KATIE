package com.example.cs442katie

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import io.opencensus.resource.Resource
import kotlinx.android.synthetic.main.course_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CourseMainAdapter(
    val context: Context,
    val intent : Intent,
    val courseList: List<Course>,
    val courseListener: (Course) -> Unit,
    val attendanceListener : (Course) -> Unit,
    val uid : String,
    val connection: ServiceConnection): RecyclerView.Adapter<CourseMainAdapter.CourseMainViewHolder> () {



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

        holder.bind(context, intent,connection, course, courseListener, attendanceListener)
    }

    class CourseMainViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val course = view.findViewById<CardView>(R.id.course)
        val courseName = view.findViewById<TextView>(R.id.course_name)
        val callAttendanceButton = view.findViewById<Button>(R.id.call_attendance_button)
        val courseId =  view.findViewById<TextView>(R.id.course_id)
        val courseInstructor = view.findViewById<TextView>(R.id.course_instructor)
        val admin = view.findViewById<TextView>(R.id.admin)
        val classEndButton = view.findViewById<Button>(R.id.class_end_button)

        fun bind(context: Context, intent: Intent, connection: ServiceConnection, courseMain: Course, courseListener: (Course) -> Unit, attendanceListener: (Course) -> Unit){
            courseName.text = courseMain.courseName
            courseId.text = courseMain.courseId
            courseInstructor.text = courseMain.instructor
//            callAttendanceButton.setOnClickListener(attendanceListener)
            course.setOnClickListener(View.OnClickListener {
                courseListener(courseMain)
            })

            classEndButton.setOnClickListener(View.OnClickListener {
                FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).get().addOnSuccessListener {
                    val lectureList = it.get("lecture") as HashMap<String, Long>
                    val studentList = it.get("student") as ArrayList<String>
                    if(lectureList.isNotEmpty()){
                        val currentLecture = lectureList
                        for (i in studentList){
                            FirebaseFirestore.getInstance().collection("users").document(i).get().addOnSuccessListener {
                                val currentClassCount = it.get("currentClassCount") as HashMap<String, Long>
                                if( currentClassCount[courseMain.courseId!!]!! > currentLecture["Check_Count"]!! * 0.8) {
                                    FirebaseFirestore.getInstance().collection("users").document(i).update("currentClassCount.${courseMain.courseId}", FieldValue.increment(1))
                                }

                            }
                        }
                    }

                }
                FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).update("isClassEnd", true)
                classEndButton.visibility = View.GONE
                if(callAttendanceButton.text == "STOP CALLING"){
                    (context as MainActivity).serviceIsBound = false
                    callAttendanceButton.text = "CALL ATTENDANCE"
                    callAttendanceButton.setBackgroundColor(Color.WHITE)
                    callAttendanceButton.setTextColor(Color.BLACK)
                    FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).update("isCheckingAttendance", false)
                    context.stopService(intent)
                    context.unbindService(connection)
                }
            })

            callAttendanceButton.setOnClickListener(View.OnClickListener {

                if(callAttendanceButton.text != "STOP CALLING"){
                    attendanceListener(courseMain)
                    FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).addSnapshotListener(MetadataChanges.INCLUDE){
                            documentSnapshot, firebaseFirestoreException ->
                        if(documentSnapshot!!.get("isCheckingAttendance") == true){
                            classEndButton.visibility = View.VISIBLE
                            callAttendanceButton.text = "STOP CALLING"
                            callAttendanceButton.setBackgroundColor(Color.BLACK)
                            callAttendanceButton.setTextColor(Color.WHITE)
                        } else {
                            callAttendanceButton.text = "CALL ATTENDANCE"
                            callAttendanceButton.setBackgroundColor(Color.WHITE)
                            callAttendanceButton.setTextColor(Color.BLACK)
                        }
                        if(documentSnapshot!!.get("isClassEnd") == true){
                            classEndButton.visibility = View.GONE
                        }
                    }
                } else {
                    (context as MainActivity).serviceIsBound = false
                    callAttendanceButton.text = "CALL ATTENDANCE"
                    callAttendanceButton.setBackgroundColor(Color.WHITE)
                    callAttendanceButton.setTextColor(Color.BLACK)
                    FirebaseFirestore.getInstance().collection("courses").document(courseMain.courseId).update("isCheckingAttendance", false)
                    context.stopService(intent)
                    context.unbindService(connection)
                }
            })
        }
    }
}