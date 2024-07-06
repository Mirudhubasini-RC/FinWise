package com.finwise.finwise_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class ExpenseActivity : Fragment() {

    private val REQUEST_EXPENSE_LOG = 100
    private val CHANNEL_ID = "100"

    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = db.collection("expense_log")
    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var firebaseAuth: FirebaseAuth



    override fun onCreateView(inflater: LayoutInflater,
                          container: ViewGroup?,
                          savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_expense, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        expenseAdapter = ExpenseAdapter()
        recyclerView.adapter = expenseAdapter
        firebaseAuth = FirebaseAuth.getInstance()

        val btnAddExpense = view.findViewById<ImageButton>(R.id.btnAddExpense)

        btnAddExpense.setOnClickListener {
            val intent = Intent(requireContext(), expense_log::class.java)
            startActivityForResult(intent, REQUEST_EXPENSE_LOG)
        }

        isnotificationenabled()
        retrieveAndDisplayExpenseData()

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXPENSE_LOG && resultCode == Activity.RESULT_OK) {
            // Handle result if needed
        }
    }

    override fun onResume() {
        super.onResume()
        retrieveAndDisplayExpenseData()
    }

    private fun retrieveAndDisplayExpenseData() {
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""
        val userExpenseRef = db.collection("Expense").document(currentUserUid)
            .collection(userName)

        userExpenseRef.get()
            .addOnSuccessListener { documents ->
                val expenseList = mutableListOf<Expense>()
                var totalSpending = 0.0

                // Create a TreeMap to automatically sort the keys (dates) in descending order
                val sortedExpenses = TreeMap<String, MutableList<Expense>>(Comparator { date1, date2 ->
                    val dateFormat = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())
                    val parsedDate1 = dateFormat.parse(date1)
                    val parsedDate2 = dateFormat.parse(date2)

                    // First, compare years
                    val yearCompare = parsedDate2!!.year.compareTo(parsedDate1!!.year)
                    if (yearCompare != 0) {
                        return@Comparator yearCompare // Sort in descending order of years
                    }

                    // Then, compare months
                    val monthCompare = parsedDate2.month.compareTo(parsedDate1.month)
                    if (monthCompare != 0) {
                        return@Comparator monthCompare // Sort in descending order of months
                    }

                    // If years and months are the same, compare the full date string
                    return@Comparator date2.compareTo(date1)
                })
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)

                for (document in documents) {
                    val id = document.id
                    val date = document.getString("Date")
                    val category = document.getString("Category")
                    val amount = document.getDouble("ExpenseAmt")

                    Log.d("ExpenseActivity", "Date: $date, Category: $category, Amount: $amount")

                    // Check if category is not null before adding expense
                    if (date != null && category != null && amount != null) {
                        // Add expense to the corresponding date group
                        val iconResourceId = getIconResource(category)
                        if (iconResourceId != null) {
                            val formattedDate = formatDate(date)
                            val expense = Expense(id,formattedDate, category, amount, iconResourceId)
                            sortedExpenses.getOrPut(formattedDate) { mutableListOf() }.add(expense)

                            val expenseYear = formattedDate.substring(formattedDate.lastIndexOf(' ') + 1).toInt()
                            val expenseMonth = SimpleDateFormat("MMMM", Locale.getDefault()).parse(formattedDate.split(",")[1].trim())?.month ?: -1


                            if (expenseYear == currentYear && expenseMonth == currentMonth) {
                                // Add the amount to total spending only for current month's expenses
                                totalSpending += amount
                            }
                        }
                    } else {
                        Log.e(
                            "ExpenseActivity",
                            "Invalid expense data: date=$date, category=$category, amount=$amount"
                        )
                    }
                }

                // Update total spending in the database
                updateTotalSpending(totalSpending)

                // Update the title with the total spending
                val totalSpendingTitle = String.format(Locale.getDefault(), "Total Spending: %.2f", totalSpending)
                requireActivity().title = totalSpendingTitle

                // Fetch and display total spending
                fetchAndDisplayTotalSpending()

                // Iterate through TreeMap entries and add expenses to the expenseList in descending order
                sortedExpenses.forEach { (_, expenses) ->
                    expenseList.addAll(expenses)
                }

                // Update RecyclerView with the sorted list of expenses
                expenseAdapter.setData(expenseList)
            }
            .addOnFailureListener { exception ->
                // Handle errors
                Log.e("ExpenseActivity", "Error getting expense documents", exception)
            }
    }
    private fun isnotificationenabled() {
        if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            // Notification permission enabled, proceed with usual operation

        } else {
            // Notification permission not enabled, prompt the user to enable it
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage("Please enable notifications to receive updates.")
                .setPositiveButton("Settings") { _, _ ->
                    // Open app settings to allow the user to enable notifications
                    val intent = Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun fetchAndDisplayTotalSpending() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userID = currentUser.uid
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR).toString()
            val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

            val userFinRef = db.collection("users").document(userID)
                .collection("finance").document(year)
                .collection(currentMonth).document("fin")

            userFinRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val spending = documentSnapshot.getDouble("spending") ?: 0.0
                    val totalSpendingText = String.format(Locale.getDefault(), "Total Spending: %.2f", spending)
                    val totalSpendingTextView = view?.findViewById<TextView>(R.id.textTotalSpending)
                    totalSpendingTextView?.text = totalSpendingText
                } else {
                    Log.e("ExpenseActivity", "Finance document for year $year, month $currentMonth does not exist")
                }
            }.addOnFailureListener { exception ->
                Log.e("ExpenseActivity", "Error fetching total spending", exception)
            }
        } else {
            Log.e("ExpenseActivity", "Current user is null")
        }
    }
    private fun updateTotalSpending(totalSpending: Double) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userID = currentUser.uid
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR).toString()
            val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

            val userFinRef = db.collection("users").document(userID)
                .collection("finance").document(year)
                .collection(currentMonth).document("fin")

            userFinRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Update the total spending field in the month's document
                    userFinRef.update("spending", totalSpending)
                        .addOnSuccessListener {
                            Log.d("ExpenseActivity", "Total spending updated successfully")
                            // Fetch and display total spending
                            fetchAndDisplayTotalSpending()
                            val budget = documentSnapshot.getDouble("budget") ?: 0.0

                            // Check if budget goes below 0
                            if (budget < 0) {
                                // Budget goes below 0, send notification
                                sendNotification("budget Exceeded")
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ExpenseActivity", "Error updating total spending", exception)
                        }
                } else {
                    Log.e("ExpenseActivity", "Finance document for year $year, month $currentMonth does not exist")
                }
            }.addOnFailureListener { exception ->
                Log.e("ExpenseActivity", "Error fetching finance document", exception)
            }
        } else {
            Log.e("ExpenseActivity", "Current user is null")
        }
    }

    private fun getIconResource(category: String?): Int? {
        return when (category) {
            "Fuel" -> R.drawable.fuel_icon
            "Electric" -> R.drawable.electric
            "Clothes","Toys","KitchenUtensils","Decoration" -> R.drawable.shop
            "Grocery","Vegetables","Fruits","Dairy Products", "Canned Goods", "Snacks", "Beverages",
            "Condiments", "Frozen Foods", "Bakery Items", "Cereals",
            "Spices", "Pasta", "Fresh Produce", "Sauces", "Masala","DosaBatter","Meat" -> R.drawable.gros_icon
            "Fees" -> R.drawable.fees
            "Mobile","Tablet","SmartWatch","Laptop" -> R.drawable.gadget
            "Rent" -> R.drawable.rent_icon
            "Fitness" -> R.drawable.fitness
            "Medical" -> R.drawable.medical
            "Pet" -> R.drawable.pet_icon
            "Movies", "Books", "Music", "Video Games" -> R.drawable.entertainment
            "Appliance","Refrigerator", "Microwave", "Washing Machine", "Dishwasher", "Blender" -> R.drawable.home_appliance
            "Investment" -> R.drawable.invest_icon
            "Loan" -> R.drawable.loan_icon
            "Notebooks", "Pens", "Pencils", "Erasers", "Markers","Colors" -> R.drawable.sationary
            "Food","Lunch","Dinner","Breakfast","Brunch","Dessert","Junk" -> R.drawable.food_icon // Both "Meat" and "Burger" map to food icon
            else -> R.drawable.coin // Default icon
        }
    }

    class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        private val expenseList = mutableListOf<Expense>()
        private val db = FirebaseFirestore.getInstance()
        private val firebaseAuth = FirebaseAuth.getInstance()

        fun setData(expenses: List<Expense>) {
            expenseList.clear()
            expenseList.addAll(expenses)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val item = expenseList[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = expenseList.size

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dateContainer: RelativeLayout = itemView.findViewById(R.id.dateContainer)
            private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
            private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
            private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
            private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
            private val separatorView: View = itemView.findViewById(R.id.separatorView)
            private val monthContainer: RelativeLayout = itemView.findViewById(R.id.monthContainer)
            private val monthTextView : TextView = itemView.findViewById(R.id.MonthTextView)
            private val yearContainer: RelativeLayout = itemView.findViewById(R.id.yearContainer)
            private val yearTextView : TextView = itemView.findViewById(R.id.yearTextView)

            init {
                // Set click listener for the entire item view
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val expense = expenseList[position]
                        showEditOrDeleteDialog(expense)
                    }
                }
            }
            private fun showEditOrDeleteDialog(expense: Expense) {
                val builder = AlertDialog.Builder(itemView.context)
                builder.setTitle("Edit or Delete")
                    .setItems(arrayOf("Edit", "Delete")) { dialog, which ->
                        when (which) {
                            0 -> showEditDialog(expense)
                            1 -> showDeleteConfirmationDialog(expense)
                        }
                    }
                    .show()
            }
            private fun showEditDialog(expense: Expense) {
                val builder = AlertDialog.Builder(itemView.context)
                val editText = EditText(itemView.context)
                editText.setText(expense.amount.toString())
                builder.setTitle("Edit Expense")
                    .setView(editText)
                    .setPositiveButton("Save") { dialog, _ ->
                        val newAmount = editText.text.toString().toDoubleOrNull()
                        if (newAmount != null) {
                            // Update the expense amount
                            updateExpenseAmount(expense, newAmount)
                        } else {
                            Toast.makeText(itemView.context, "Invalid amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            private fun updateExpenseAmount(expense: Expense, newAmount: Double) {
                val currentUserUid = firebaseAuth.currentUser?.uid ?: return
                val currentUser = firebaseAuth.currentUser
                val userName = currentUser?.displayName ?: ""

                // Reference to the user's expense collection
                val userExpenseRef = db.collection("Expense").document(currentUserUid)
                    .collection(userName)

                // Parse and format the expense date
                val formattedDate = formatDate(expense.date)

                // Construct the query to find the matching expense document
                userExpenseRef
                    .whereEqualTo("Category", expense.category)
                    .whereEqualTo("Date", formattedDate)
                    .whereEqualTo("ExpenseAmt", expense.amount)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        // Iterate through the query results
                        for (document in querySnapshot.documents) {
                            // Get the reference to the document and update it
                            val expenseDocumentRef = document.reference
                            expenseDocumentRef.update("ExpenseAmt", newAmount)
                                .addOnSuccessListener {
                                    // Successful update
                                    val updatedExpense = expense.copy(amount = newAmount)
                                    val position = expenseList.indexOf(expense)
                                    if (position != -1) {
                                        expenseList[position] = updatedExpense
                                        notifyItemChanged(position)
                                        Log.d("ExpenseAdapter", "Expense amount updated successfully")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    // Error handling for update failure
                                    Log.e("ExpenseAdapter", "Error updating expense amount", exception)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Error handling for query failure
                        Log.e("ExpenseAdapter", "Error retrieving expense documents", exception)
                    }
            }

            private fun showDeleteConfirmationDialog(expense: Expense) {
                val builder = AlertDialog.Builder(itemView.context)
                builder.setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setPositiveButton("Delete") { _, _ ->
                        // Call a method to delete the expense
                        deleteExpense(expense)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            private fun deleteExpense(expense: Expense) {
                val currentUserUid = firebaseAuth.currentUser?.uid ?: return
                val currentUser = firebaseAuth.currentUser
                val userName = currentUser?.displayName ?: ""

                // Reference to the user's expense collection
                val userExpenseRef = db.collection("Expense").document(currentUserUid)
                    .collection(userName)

                // Parse and format the expense date
                val formattedDate = formatDate(expense.date)
                Log.d("formatted","$formattedDate")
                // Construct the query to find the matching expense document
                userExpenseRef
                    .whereEqualTo("Category", expense.category)
                    .whereEqualTo("Date", formattedDate)
                    .whereEqualTo("ExpenseAmt", expense.amount)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        // Iterate through the query results
                        for (document in querySnapshot.documents) {
                            // Get the reference to the document and delete it
                            val expenseDocumentRef = document.reference
                            expenseDocumentRef.delete()
                                .addOnSuccessListener {
                                    // Successful deletion
                                    val position = expenseList.indexOf(expense)
                                    if (position != -1) {
                                        expenseList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemRangeChanged(position, expenseList.size)
                                        Log.d("Delete", "Expense deleted successfully")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    // Error handling for deletion failure
                                    Log.e("ExpenseAdapter", "Error deleting expense", exception)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Error handling for query failure
                        Log.e("ExpenseAdapter", "Error retrieving expense documents", exception)
                    }

            }

            private fun formatDate(dateString: String): String {
                // Define input and output date formats
                val inputDateFormat = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("yyyy-M-d", Locale.getDefault())

                return try {
                    // Parse the input date string into a Date object
                    val date = inputDateFormat.parse(dateString!!)
                    outputDateFormat.format(date!!)
                } catch (e: Exception) {
                    // Return the original date string in case of an error
                    dateString
                }
            }


            fun bind(expense: Expense) {
                // Bind data to views
                dateTextView.text = expense.date
                iconImageView.setImageResource(expense.iconResourceId)
                categoryTextView.text = expense.category
                amountTextView.text = String.format(Locale.getDefault(), "%.2f", expense.amount)

                val year = getYearFromDate(expense.date)
                yearTextView.text = year
                val month = getMonthFromDate(expense.date)
                monthTextView.text = month

                if (adapterPosition == 0) {
                    // For the first item, always make yearContainer visible
                    yearContainer.visibility = View.VISIBLE
                } else {
                    val previousExpense = expenseList[adapterPosition - 1]
                    val previousYear = getYearFromDate(previousExpense.date)
                    if (year != previousYear) {
                        yearContainer.visibility = View.VISIBLE
                    } else {
                        yearContainer.visibility = View.GONE
                    }
                }
                if (adapterPosition == 0) {
                    // For the first item, always make monthContainer visible
                    monthContainer.visibility = View.VISIBLE
                } else {
                    val previousExpense = expenseList[adapterPosition - 1]
                    val previousMonth = getMonthFromDate(previousExpense.date)
                    if (month != previousMonth) {
                        monthContainer.visibility = View.VISIBLE
                    } else {
                        monthContainer.visibility = View.GONE
                    }
                }
                // Check if the current date is different from the previous one
                if (adapterPosition == 0) {
                    // For the first item, always make dateContainer visible
                    dateContainer.visibility = View.VISIBLE
                } else {
                    // For other items, show or hide dateContainer based on the comparison of dates
                    if (expenseList[adapterPosition - 1].date != expense.date) {
                        dateContainer.visibility = View.VISIBLE
                    } else {
                        dateContainer.visibility = View.GONE
                    }
                }

                // Show or hide separator based on your logic
                if (adapterPosition < expenseList.size - 1 && expenseList[adapterPosition + 1].date != expense.date) {
                    // Show separator if the current item's date is different from the next item's date
                    separatorView.visibility = View.VISIBLE
                } else {
                    // Hide separator otherwise
                    separatorView.visibility = View.GONE
                }
            }
        }



        private fun getMonthFromDate(date: String): String {
            val inputFormat = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())
            val dateObject = inputFormat.parse(date)
            val outputFormat = SimpleDateFormat("MMMM", Locale.getDefault())
            return outputFormat.format(dateObject)
        }
        private fun getYearFromDate(date: String): String {
            val inputFormat = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())
            val dateObject = inputFormat.parse(date)
            val outputFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            return outputFormat.format(dateObject)
        }
    }


    data class Expense(val id: String,val date: String, val category: String, val amount: Double, val iconResourceId: Int)

    private fun formatDate(dateString: String?): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Assuming the date format from Firestore
        val outputFormat = SimpleDateFormat("dd, MMMM yyyy", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateString!!)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            // Handle parse error
            "Invalid Date"
        }
    }


    private fun sendNotification(message: String) {
        val notificationId = 1
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.sharp_add_alert_24) // Set your notification icon
            .setContentTitle("Budget Limit Exceeded")
            .setContentText("Dear User, Please me mindful about what you spend")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(requireContext())) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }

}


