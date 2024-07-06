package com.finwise.finwise_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentReference
import java.util.*

class profile : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser
    private val db = FirebaseFirestore.getInstance()
    private lateinit var addIncomeEditText: EditText
    private lateinit var addBudgetEditText: EditText
    private lateinit var totalSavingsEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.activity_profile, container, false)
        firestore = FirebaseFirestore.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser!!

        val hiTextView = view.findViewById<TextView>(R.id.username)
        addIncomeEditText = view.findViewById(R.id.AddIncome)
        addBudgetEditText = view.findViewById(R.id.AddBudget)
        totalSavingsEditText = view.findViewById(R.id.TotalSavings)
        val saveButton = view.findViewById<Button>(R.id.Savebtn)

        hiTextView.text = "Hi ${currentUser.displayName}"
        fetchFinanceData()



        saveButton.setOnClickListener {
            // Ensure views are not null before accessing them
            val income = addIncomeEditText.text.toString().toDoubleOrNull() ?: 0.0
            val budget = addBudgetEditText.text.toString().toDoubleOrNull() ?: 0.0

            fetchTotalSpending { totalSpending ->
                val updatedBudget = if (budget > 0) budget - totalSpending else 0.0
                val savings = income - totalSpending
                totalSavingsEditText.setText(savings.toString())
                // Call function to update Firestore
                updateFinanceData(income, updatedBudget, savings, totalSpending)
            }
        }

        val editSymbol: ImageView = view.findViewById(R.id.edit_symbol)

        editSymbol.setOnClickListener {
            // Start Profile_Edit activity when the edit_symbol ImageView is clicked
            val intent = Intent(requireContext(), Profile_edit::class.java)
            startActivityForResult(intent, PROFILE_EDIT_REQUEST_CODE)
        }

        return view
    }
    override fun onResume() {
        super.onResume()
        fetchFinanceData()
    }


    private fun fetchFinanceData() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

        // Reference to the document in Firestore
        val financeDocRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("finance")
            .document(year)
            .collection(month)
            .document("fin")

        // Fetch finance data from Firestore
        financeDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val income = document.getDouble("income") ?: 0.0
                    val budget = document.getDouble("budget") ?: 0.0
                    val totalSpending = document.getDouble("spending") ?: 0.0
                    val savings = if (income > 0 && budget > 0) income - totalSpending else 0.0

                    // Calculate updated budget
                    val updatedBudget = if (budget > 0) budget - totalSpending else 0.0

                    // Display income, budget, and savings
                    addIncomeEditText.setText(income.toString())
                    addBudgetEditText.setText(updatedBudget.toString())
                    totalSavingsEditText.setText(savings.toString())

                    updateSavings(financeDocRef, savings)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching finance data", e)
            }
    }
    private fun updateSavings(financeDocRef: DocumentReference, savings: Double) {
        // Create data to be updated
        val financeData = hashMapOf(
            "savings" to savings
        )

        // Update Firestore document
        financeDocRef.update(financeData as Map<String, Any>)
            .addOnSuccessListener {
                // Handle success
                Log.d(TAG, "Savings updated successfully in Firestore")
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e(TAG, "Error updating savings in Firestore", e)
            }
    }

    private fun fetchTotalSpending(callback: (Double) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

        // Reference to the document in Firestore
        val financeDocRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("finance")
            .document(year)
            .collection(month)
            .document("fin")

        // Fetch spending value from Firestore
        financeDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalSpending = document.getDouble("spending") ?: 0.0
                    callback(totalSpending)
                } else {
                    callback(0.0)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching total spending", e)
                callback(0.0)
            }
    }

    private fun updateFinanceData(income: Double, budget: Double, savings: Double, totalSpending: Double) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)

        // Reference to the document in Firestore
        val financeDocRef = firestore.collection("users")
            .document(currentUser.uid)
            .collection("finance")
            .document(year)
            .collection(month)
            .document("fin")

        // Create data to be updated
        val financeData = hashMapOf(
            "income" to income,
            "budget" to budget,
            "savings" to savings,
            "spending" to totalSpending
        )

        // Update Firestore document
        financeDocRef.set(financeData)
            .addOnSuccessListener {
                // Handle success
                Log.d(TAG, "Finance data updated successfully")
                // Show toast message
                Toast.makeText(
                    requireContext(),
                    "Finance data updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                // Check if spending exceeds budget
                if (budget < 0) {
                    // Send notification
                    sendNotification("You have exceeded your budget!")
                }
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e(TAG, "Error updating finance data", e)
            }
    }

    private fun sendNotification(message: String) {
        val notificationId = 1
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.sharp_add_alert_24) // Set your notification icon
            .setContentTitle("Budget Limit Exceeded ;_; ")
            .setContentText("Dear User, Please be mindful about what you spend")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(requireContext())) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }

    companion object {
        private const val TAG = "ProfileFragment"
        private const val CHANNEL_ID = "100"
        private const val PROFILE_EDIT_REQUEST_CODE = 101
    }
}
