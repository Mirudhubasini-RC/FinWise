package com.finwise.finwise_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.widget.Switch
import android.app.PendingIntent
import java.text.SimpleDateFormat
import android.app.AlertDialog
import java.util.Locale
import android.content.Context
import android.widget.Toast
import android.app.Activity
import android.app.AlarmManager
import android.net.Uri
import android.os.Build
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


class reminder_log : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var plusButton: ImageButton
    private lateinit var adapter: ReminderAdapter
    private val reminderList = mutableListOf<Reminder>()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var fragmentContext: Context
    private lateinit var googlePayLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Assign the context to the fragmentContext variable
        fragmentContext = context

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("onCreateView","im called")
        val view = inflater.inflate(R.layout.activity_reminder_log, container, false)
        recyclerView = view.findViewById(R.id.recyclerView3)
        plusButton = view.findViewById(R.id.plusButton)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ReminderAdapter(reminderList)
        recyclerView.adapter = adapter

        googlePayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Payment completed successfully
                Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                // Handle any post-payment actions here
            } else {
                // Payment was not successful or user canceled
                Toast.makeText(context, "Payment failed or canceled", Toast.LENGTH_SHORT).show()
                // Handle the failure or cancellation scenario
            }
        }

        setupPlusButton()
        fetchAndDisplayReminders()
        return view


    }
    override fun onResume() {
        super.onResume()
        fetchAndDisplayReminders()
    }

    private fun setupPlusButton() {
        plusButton.setOnClickListener {
            val intent = Intent(requireContext(), SetReminder::class.java)
            startActivity(intent)
        }
    }
    private fun fetchAndDisplayReminders() {
        Log.d("FetchAndDisp", "I am working")

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val userName = currentUser?.displayName

        if (userId != null && userName != null) {
            val reminderRef = db.collection("reminder")
                .document(userId)
                .collection(userName)

            reminderRef.get()
                .addOnSuccessListener { result ->
                    reminderList.clear()
                    for (document in result) {
                        val id = document.id
                        val time = document.getString("Time") ?: ""
                        val date = document.getString("Date") ?: ""
                        val description = document.getString("Description") ?: ""
                        val label = document.getString("Label") ?: ""
                        Log.d("reminder_log", "Date: $date, Description: $description, Label: $label")
                        reminderList.add(Reminder(id,time,date, description, label))

                    }
                    Log.d("reminder_log", "Reminder List Size: ${reminderList.size}")

                    adapter.notifyDataSetChanged() // Notify adapter about data change
                }
                .addOnFailureListener { exception ->
                    Log.e("reminder_log", "Error fetching reminders", exception)
                }
        } else {
            Log.e("reminder_log", "User ID or User Name is null")
        }
    }



    inner class ReminderAdapter(private val reminderList: List<Reminder>) :
        RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
            return ReminderViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
            val reminder = reminderList[position]
            holder.bind(reminder)
        }

        override fun getItemCount(): Int = reminderList.size

        inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val alarmIconImageView: ImageView = itemView.findViewById(R.id.alarmIcon)
            private val alarmTitleTextView: TextView = itemView.findViewById(R.id.alarmTitle)
            private val alarmSwitch: Switch = itemView.findViewById(R.id.alarmSwitch)
            private val Date: TextView = itemView.findViewById(R.id.date)
            private lateinit var currentReminder: Reminder
            private val alarmManager: AlarmManager =
                context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            init {

                itemView.setOnClickListener {
                    Log.d("Click Pressed","Detected")
                    showOptionsDialog(currentReminder)
                }

                alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Schedule alarm
                        scheduleAlarm(currentReminder)
                    } else {
                        // Cancel alarm
                        cancelAlarm(currentReminder.id)
                    }
                }
            }
            fun bind(reminder: Reminder) {
                currentReminder = reminder
                alarmIconImageView.setImageResource(R.drawable.ic_alarm)
                alarmTitleTextView.text = reminder.label
                Date.text = "Due is on ${reminder.date}"
                alarmSwitch.isChecked = isAlarmScheduled(reminder.id)
            }
            fun showOptionsDialog(reminder: Reminder) {
                val options = arrayOf("Edit", "Delete", "Pay by Cash", "Pay through UPI")

                val builder = AlertDialog.Builder(itemView.context)
                builder.setTitle("Select Action")
                builder.setItems(options) { dialog, which ->
                    when (which) {
                        0 -> editAlarm(reminder)       // Edit option
                        1 -> deleteAlarm(reminder)     // Delete option
                        2 -> payByCash(reminder)       // Pay by Cash option
                        3 -> showPayThroughUPIDialog(reminder)   // Pay through UPI option
                    }
                    dialog.dismiss()
                }

                builder.show()
            }

            private fun showPayThroughUPIDialog(reminder: Reminder) {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_layout, null)
                val builder = AlertDialog.Builder(context)
                builder.setView(dialogView)
                builder.setTitle("Pay through UPI")

                val amountEditText: EditText = dialogView.findViewById(R.id.amountEditText)
                val upiIdEditText: EditText = dialogView.findViewById(R.id.upiIdEditText)
                val recipientNameEditText: EditText = dialogView.findViewById(R.id.recipientNameEditText)

                builder.setPositiveButton("Pay") { dialog, _ ->
                    val amountText = amountEditText.text.toString()
                    val upiId = upiIdEditText.text.toString()
                    val recipientName = recipientNameEditText.text.toString()

                    if (amountText.isNotBlank() && upiId.isNotBlank() && recipientName.isNotBlank()) {

                        payThroughUPI(reminder, amountText, upiId, recipientName)
                    } else {
                        Toast.makeText(context, "Please enter amount, UPI ID, and recipient name", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }

                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog = builder.create()
                dialog.show()
            }

            private fun payThroughUPI(reminder: Reminder, amount: String, upiId: String, recipientName: String) {
                val GOOGLE_PAY_PACKAGE_NAME = "com.google.android.apps.nbu.paisa.user"
                val GOOGLE_PAY_REQUEST_CODE = 123
                // Construct the URI for UPI payment
                val uri: Uri = Uri.Builder()
                    .scheme("upi")
                    .authority("pay")
                    .appendQueryParameter("pa", upiId)
                    .appendQueryParameter("pn", recipientName)
                    .appendQueryParameter("tr", "25584584")
                    .appendQueryParameter("mc", "")
                    .appendQueryParameter("am", amount)
                    .appendQueryParameter("cu", "INR")
                    .build()

                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = uri
                intent.setPackage(GOOGLE_PAY_PACKAGE_NAME)

                try {
                    googlePayLauncher.launch(intent) // Launch the activity using the Activity Result Launcher
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle exceptions, for example, Google Pay not installed or other errors
                }
            }


            private fun editAlarm(reminder: Reminder) {
                val context = itemView.context
                val intent = Intent(context, SetReminder::class.java).apply {
                    putExtra("reminderId", reminder.id)
                    putExtra("label", reminder.label)
                    putExtra("date", reminder.date)
                    putExtra("time", reminder.time)
                    putExtra("description", reminder.description)
                }
                context.startActivity(intent)
            }
            private fun payByCash(reminder: Reminder){
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid
                val userName = currentUser?.displayName

                if (userId != null && userName != null) {
                    val reminderRef = db.collection("reminder")
                        .document(userId)
                        .collection(userName)
                        .document(reminder.id)

                    reminderRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Reminder is paid thorough Cash", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReminderViewHolder", "Error cancelling alarm", e)
                            Toast.makeText(context, "Failed to cancel alarm", Toast.LENGTH_SHORT).show()
                        }
                }
                fetchAndDisplayReminders()
            }
            private fun deleteAlarm(reminder: Reminder) {
                // Example: Call a method to delete the alarm from Firestore
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid
                val userName = currentUser?.displayName

                if (userId != null && userName != null) {
                    val reminderRef = db.collection("reminder")
                        .document(userId)
                        .collection(userName)
                        .document(reminder.id)

                    reminderRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Reminder deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReminderViewHolder", "Error deleting alarm", e)
                            Toast.makeText(context, "Failed to delete alarm", Toast.LENGTH_SHORT).show()
                        }
                }
                fetchAndDisplayReminders()
            }

            private fun isAlarmScheduled(reminderId: String): Boolean {
                // Check if alarm is scheduled for this reminderId
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminderId.hashCode(),
                    intent,
                    pendingIntentFlags
                )
                return pendingIntent != null
            }

            private fun scheduleAlarm(reminder: Reminder) {
                val dateTimeStr = "${reminder.date} ${reminder.time}"
                val sdf = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())
                val dateTime = sdf.parse(dateTimeStr)

                if (dateTime != null) {
                    if (dateTime.time <= System.currentTimeMillis()) {
                        Log.d("ReminderAdapter", "Skipping alarm schedule for past dateTime: $dateTimeStr")
                        return
                    }

                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("label", reminder.label)
                        putExtra("description",reminder.description)
                        putExtra("reminderId", reminder.id)
                    }

                    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val canScheduleExact = alarmManager.canScheduleExactAlarms()
                        if (canScheduleExact) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminder.id.hashCode(),
                        alarmIntent,
                        pendingIntentFlags
                    )

                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        dateTime.time,
                        pendingIntent
                    )
                    Log.d("ReminderAdapter", "Alarm scheduled for $dateTimeStr")
                }
            }
            private fun cancelAlarm(reminderId: String) {
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminderId.hashCode(),
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("ReminderAdapter", "Alarm canceled for reminder ID: $reminderId")
            }
        }
    }

    data class Reminder(val id: String,val time: String,val date: String, val description: String, val label: String)
}
