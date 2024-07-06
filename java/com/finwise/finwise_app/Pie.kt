package com.finwise.finwise_app


import android.os.Bundle
import android.graphics.Color
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.widget.EditText
import androidx.appcompat.app.AlertDialog


class Pie : Fragment() {

    private lateinit var tvTitle: TextView
    private lateinit var spinnerInterval: Spinner
    private lateinit var pieChart: PieChart
    private lateinit var context: Context
    private val dbroot = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_pie, container, false)

        tvTitle = view.findViewById(R.id.tvTitle)
        spinnerInterval = view.findViewById(R.id.spinnerInterval)
        pieChart = view.findViewById(R.id.pieChart)

        // Initialize spinner options
        val intervalOptions = arrayOf("Month", "Week", "Day","Duration")
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.interval_options))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = adapter

        // Set default selection to "Month"
        spinnerInterval.setSelection(0)

        // Handle spinner item selection
        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedInterval = parent?.getItemAtPosition(position).toString()
                if (selectedInterval == "Duration") {
                    showDurationDialog() // This is where the function is called
                } else {
                    loadPieChart(selectedInterval)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Load default pie chart (Monthly)
        loadPieChart("Month")

        return view
    }
    private fun showDurationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_duration, null)
        val etFromDate = dialogView.findViewById<EditText>(R.id.etFromDate)
        val etToDate = dialogView.findViewById<EditText>(R.id.etToDate)

        val fromDateCalendar = Calendar.getInstance()
        val toDateCalendar = Calendar.getInstance()

        val fromDateListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            fromDateCalendar.set(Calendar.YEAR, year)
            fromDateCalendar.set(Calendar.MONTH, monthOfYear)
            fromDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            val fromDateText = dateFormatter.format(fromDateCalendar.time)
            etFromDate.setText(fromDateText)
        }

        val toDateListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            toDateCalendar.set(Calendar.YEAR, year)
            toDateCalendar.set(Calendar.MONTH, monthOfYear)
            toDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            val toDateText = dateFormatter.format(toDateCalendar.time)
            etToDate.setText(toDateText)
        }

        val fromDateDialog = DatePickerDialog(
            requireContext(),
            fromDateListener,
            fromDateCalendar.get(Calendar.YEAR),
            fromDateCalendar.get(Calendar.MONTH),
            fromDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val toDateDialog = DatePickerDialog(
            requireContext(),
            toDateListener,
            toDateCalendar.get(Calendar.YEAR),
            toDateCalendar.get(Calendar.MONTH),
            toDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        etFromDate.setOnClickListener {
            fromDateDialog.show()
        }

        etToDate.setOnClickListener {
            toDateDialog.show()
        }

        AlertDialog.Builder(context)
            .setTitle("Select Duration")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val fromDateText = etFromDate.text.toString()
                val toDateText = etToDate.text.toString()
                if (fromDateText.isNotEmpty() && toDateText.isNotEmpty()) {
                    val duration = Pair(fromDateText, toDateText)
                    loadPieChart("Duration", duration)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Please select both dates", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun formatDate(year: Int, month: Int, dayOfMonth: Int): String {
        return "$year/${month + 1}/$dayOfMonth"
    }
    private fun loadPieChart(interval: String,duration: Pair<String, String>?=null) {
        // TODO: Implement loading of pie chart based on selected interval
        // You can update the title and load pie chart data accordingly
        when (interval) {
            "Month" -> {
                tvTitle.text = "Monthly Expenses"
                // Load pie chart for monthly expenses
                fetchMonthData()
            }
            "Week" -> {
                tvTitle.text = "Weekly Expenses"
                // Load pie chart for weekly expenses
                fetchWeekData()
            }
            "Day" -> {
                tvTitle.text = "Daily Expenses"
                // Load pie chart for daily expenses
                fetchDayData()
            }
            "Duration" -> {
                val fromDate = duration?.first ?: return
                val toDate = duration.second ?: return
                tvTitle.text = "Expenses-$fromDate-$toDate"
                // Load pie chart for expenses within the specified duration
                fetchDurationData(fromDate, toDate)
            }
        }
    }
    private fun fetchMonthData()
    {
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""

        if (currentUserUid != null) {
            dbroot.collection("Expense").document(currentUserUid)
                .collection(userName)
                .get()
                .addOnCompleteListener { task: Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val documentSnapshots = task.result
                        if (documentSnapshots != null && !documentSnapshots.isEmpty) {
                            val expenseData = mutableMapOf<String, Double>()
                            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                            for (documentSnapshot in documentSnapshots) {
                                val category = documentSnapshot.getString("Category")
                                val date = documentSnapshot.getString("Date")
                                val expenseAmt = documentSnapshot.getDouble("ExpenseAmt")
                                if (category != null && date != null && expenseAmt != null) {
                                    val dateStr = documentSnapshot.getString("Date")
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val date = dateFormat.parse(dateStr)
                                    val expenseCalendar = Calendar.getInstance()
                                    expenseCalendar.time = date
                                    val expenseMonth = expenseCalendar.get(Calendar.MONTH) + 1
                                    if (expenseMonth == currentMonth) {
                                        expenseData[category] = (expenseData[category] ?: 0.0) + expenseAmt
                                    }
                                }
                            }
                            drawPieChart(expenseData)
                        } else {
                            pieChart.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_LONG).show()
        }
    }
    private fun fetchWeekData()
    {
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""

        if (currentUserUid != null) {
            dbroot.collection("Expense").document(currentUserUid)
                .collection(userName)
                .get()
                .addOnCompleteListener { task: Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val documentSnapshots = task.result
                        if (documentSnapshots != null && !documentSnapshots.isEmpty) {
                            val expenseData = mutableMapOf<String, Double>()
                            val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
                            for (documentSnapshot in documentSnapshots) {
                                val category = documentSnapshot.getString("Category")
                                val date = documentSnapshot.getString("Date")
                                val expenseAmt = documentSnapshot.getDouble("ExpenseAmt")
                                if (category != null && date != null && expenseAmt != null) {
                                    val dateStr = documentSnapshot.getString("Date")
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val date = dateFormat.parse(dateStr)
                                    val expenseCalendar = Calendar.getInstance()
                                    expenseCalendar.time = date
                                    val expenseWeek = expenseCalendar.get(Calendar.WEEK_OF_YEAR)
                                    if (expenseWeek == currentWeek) {
                                        expenseData[category] = (expenseData[category] ?: 0.0) + expenseAmt
                                    }
                                }
                            }
                            drawPieChart(expenseData)
                        } else {
                            pieChart.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_LONG).show()
        }
    }
    private fun fetchDayData()
    {
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""

        if (currentUserUid != null) {
            dbroot.collection("Expense").document(currentUserUid)
                .collection(userName)
                .get()
                .addOnCompleteListener { task: Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val documentSnapshots = task.result
                        if (documentSnapshots != null && !documentSnapshots.isEmpty) {
                            val expenseData = mutableMapOf<String, Double>()
                            val currentDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                            for (documentSnapshot in documentSnapshots) {
                                val category = documentSnapshot.getString("Category")
                                val date = documentSnapshot.getString("Date")
                                val expenseAmt = documentSnapshot.getDouble("ExpenseAmt")
                                if (category != null && date != null && expenseAmt != null) {
                                    val dateStr = documentSnapshot.getString("Date")
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val date = dateFormat.parse(dateStr)
                                    val expenseCalendar = Calendar.getInstance()
                                    expenseCalendar.time = date
                                    val expenseDay = expenseCalendar.get(Calendar.DAY_OF_YEAR)
                                    if (expenseDay == currentDate) {
                                        expenseData[category] = (expenseData[category] ?: 0.0) + expenseAmt
                                    }
                                }
                            }
                            drawPieChart(expenseData)
                        } else {
                            pieChart.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_LONG).show()
        }
    }
    private fun fetchDurationData(fromDate: String, toDate: String) {
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""

        if (currentUserUid != null) {
            dbroot.collection("Expense").document(currentUserUid)
                .collection(userName)
                .get()
                .addOnCompleteListener { task: Task<QuerySnapshot> ->
                    if (task.isSuccessful) {
                        val documentSnapshots = task.result
                        if (documentSnapshots != null && !documentSnapshots.isEmpty) {
                            val expenseData = mutableMapOf<String, Double>()
                            val fromDateMillis = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(fromDate)?.time ?: return@addOnCompleteListener
                            val toDateMillis = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(toDate)?.time ?: return@addOnCompleteListener

                            for (documentSnapshot in documentSnapshots) {
                                val category = documentSnapshot.getString("Category")
                                val dateStr = documentSnapshot.getString("Date")
                                val expenseAmt = documentSnapshot.getDouble("ExpenseAmt")

                                if (category != null && dateStr != null && expenseAmt != null) {
                                    val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                                    val expenseTimeInMillis = expenseDate?.time ?: continue

                                    if (expenseTimeInMillis in fromDateMillis..toDateMillis) {
                                        expenseData[category] = (expenseData[category] ?: 0.0) + expenseAmt
                                    }
                                }
                            }
                            drawPieChart(expenseData)
                        } else {
                            pieChart.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_LONG).show()
        }
    }
    private fun drawPieChart(expenseData: Map<String, Double>) {
        val entries = ArrayList<PieEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        var totalExpense = 0.0
        val customColors = intArrayOf(
            Color.rgb(255, 102, 0),   // Orange
            Color.rgb(51, 153, 255),  // Blue
            Color.rgb(255, 51, 153),  // Pink
            Color.rgb(102, 255, 51),  // Green
            Color.rgb(255, 153, 51),  // Amber
            Color.rgb(153, 51, 255),  // Purple
            Color.rgb(51, 255, 204),  // Turquoise
            Color.rgb(255, 204, 51),  // Yellow
            Color.rgb(102, 153, 255), // Light Blue
            Color.rgb(255, 51, 102),  // Red
            Color.rgb(51, 255, 102),  // Lime
            Color.rgb(204, 51, 255),  // Deep Purple
            Color.rgb(255, 204, 204), // Pink
            Color.rgb(204, 255, 51),  // Lime
            Color.rgb(255, 102, 153), // Deep Orange
            Color.rgb(51, 204, 255),  // Sky Blue
            Color.rgb(255, 204, 102), // Amber
            Color.rgb(102, 255, 153), // Light Green
            Color.rgb(204, 102, 255), // Indigo
            Color.rgb(255, 153, 204)  // Pink
        )


        var colorIndex = 0
        for ((category, amount) in expenseData) {
            entries.add(PieEntry(amount.toFloat()))
            labels.add(category)
            totalExpense += amount

            // Add color from custom color array
            colors.add(customColors[colorIndex])

            // Increment color index and ensure it stays within the bounds of the custom color array
            colorIndex = (colorIndex + 1) % customColors.size
        }

        val dataSet = PieDataSet(entries, "Expenses")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1400)
        pieChart.legend.isEnabled = true
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setUsePercentValues(true)
        pieChart.centerText = "Total\n${String.format("%.2f", totalExpense)}"
        pieChart.setCenterTextSize(18f)
        pieChart.setDrawEntryLabels(true)

        // Set labels for the entries
        data.setDrawValues(true)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(12f)
        data.setValueTextColor(Color.BLACK)

        // Set labels for each entry
        for ((index, entry) in entries.withIndex()) {
            entry.label = labels[index]
        }

        pieChart.invalidate()
    }
}