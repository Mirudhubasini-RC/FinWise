package com.finwise.finwise_app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build
import android.util.Log
import android.widget.ImageView
import java.text.ParseException

class SetReminder : AppCompatActivity() {

    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var labelEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var reminderId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_reminder)

        // Initialize views
        dateEditText = findViewById(R.id.dateEditText)
        timeEditText = findViewById(R.id.timeEditText)
        labelEditText = findViewById(R.id.labelEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        saveButton = findViewById(R.id.setButton)

        if (intent.hasExtra("reminderId")) {
            // Retrieve reminder details from intent extras
            reminderId = intent.getStringExtra("reminderId")
            val label = intent.getStringExtra("label")
            val date = intent.getStringExtra("date")
            val time = intent.getStringExtra("time")
            val description = intent.getStringExtra("description")

            // Prepopulate EditTexts with reminder details
            labelEditText.setText(label)
            dateEditText.setText(date)
            timeEditText.setText(time)
            descriptionEditText.setText(description)
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Finish current activity to navigate back
        }

        // Set OnClickListener for dateEditText to show DatePickerDialog
        dateEditText.setOnClickListener {
            showDatePicker()
        }

        // Set OnClickListener for timeEditText to show TimePickerDialog
        timeEditText.setOnClickListener {
            showTimePicker()
        }

        // Set OnClickListener for saveButton
        saveButton.setOnClickListener {
           saveReminderToFirestore()
        }
    }

    private fun showDatePicker() {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                dateEditText.setText(sdf.format(selectedDate.time))
            }, year, month, day
        )

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis()

        datePicker.show()
    }

    private fun showTimePicker() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedTime.set(Calendar.MINUTE, selectedMinute)
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                timeEditText.setText(sdf.format(selectedTime.time))
            }, hour, minute, false
        )

        timePicker.show()
    }

    private fun saveReminderToFirestore() {
        // You can perform validation here if necessary

        // Continue with saving the reminder to Firestore
        val time = timeEditText.text.toString().trim()
        val label = labelEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        // Get the current user's UID and name
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val userName = currentUser?.displayName

        if (userId != null && userName != null) {
            val reminder = hashMapOf(
                "Date" to dateEditText.text.toString().trim(),
                "Time" to time,
                "Label" to label,
                "Description" to description
            )

            val collection = db.collection("reminder")
                .document(userId)
                .collection(userName)

            if (reminderId != null) {
                // Update existing reminder
                collection.document(reminderId!!)
                    .update(reminder as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reminder updated successfully!", Toast.LENGTH_SHORT).show()
                        scheduleAlarm(dateEditText.text.toString().trim(), time, label,description, reminderId!!)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update reminder!", Toast.LENGTH_SHORT).show()
                        Log.e("SetReminder", "Error updating reminder", e)
                    }
            } else {
                // Add new reminder
                collection.add(reminder)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Reminder saved successfully!", Toast.LENGTH_SHORT).show()
                        scheduleAlarm(dateEditText.text.toString().trim(), time, label, description,documentReference.id)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save reminder!", Toast.LENGTH_SHORT).show()
                        Log.e("SetReminder", "Error saving reminder", e)
                    }
            }
        } else {
            // User is not authenticated or user's UID is null
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun scheduleAlarm(date: String, time: String, label: String,description:String, reminderId: String) {
        val dateTimeStr = "$date $time"
        val sdf = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())

        try {
            val dateTime = sdf.parse(dateTimeStr)
            if (dateTime != null) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java).apply {
                    putExtra("label", label)
                    putExtra("description", description)
                    putExtra("reminderId", reminderId)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    reminderId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                // Check if the app can schedule exact alarms
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val canScheduleExact = alarmManager.canScheduleExactAlarms()
                    if (!canScheduleExact) {
                        // Handle the case where the app cannot schedule exact alarms
                        Toast.makeText(this, "Cannot schedule exact alarms", Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        dateTime.time,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        dateTime.time,
                        pendingIntent
                    )
                }
                val formattedDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(dateTime)
                Log.d("SetReminder", "Alarm scheduled for $formattedDateTime")
                Toast.makeText(this, "Alarm scheduled for $formattedDateTime", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid date/time format!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ParseException) {
            Toast.makeText(this, "Error parsing date/time!", Toast.LENGTH_SHORT).show()
        }
    }


}