package com.finwise.finwise_app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class expense_log : AppCompatActivity() {

    private lateinit var editAmount: EditText
    private lateinit var editDate: EditText
    private lateinit var buttonDone: Button
    private lateinit var addbtn: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var selectedImageView: ImageView

    private var selectedCategory = ""

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_log)

        editAmount = findViewById(R.id.editAmount)
        editDate = findViewById(R.id.editDate)
        buttonDone = findViewById(R.id.buttonDone)
        firebaseAuth = FirebaseAuth.getInstance()
        addbtn = findViewById(R.id.buttonSelectCategory)
        selectedImageView = findViewById(R.id.selectedCategoryIcon)

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Finish current activity to navigate back
        }



        setupDatePicker()

        buttonDone.setOnClickListener {
            setResult(Activity.RESULT_OK)
            if (validateFields()) {
                saveExpenseData()
                finish()
            } else {
                // Handle case where fields are not valid (e.g., display an error message)
            }
        }

        addbtn.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    private fun setupDatePicker() {
        editDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = "$year-${monthOfYear + 1}-$dayOfMonth"
                editDate.setText(selectedDate)
            }, currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }

    private fun validateFields(): Boolean {
        val amount = editAmount.text.toString().toDoubleOrNull()
        val date = editDate.text.toString()

        if (amount == null || amount <= 0) {
            editAmount.error = "Invalid amount"
            return false
        }

        if (date.isEmpty()) {
            editDate.error = "Please select a date"
            return false
        }

        if (selectedCategory.isEmpty()) {
            // Display a toast message indicating that a category needs to be selected
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveExpenseData() {
        val amount = editAmount.text.toString().toDouble()
        val date = editDate.text.toString()
        val currentUserUid = firebaseAuth.currentUser?.uid ?: return
        val currentUser = firebaseAuth.currentUser
        val userName = currentUser?.displayName ?: ""

        val expenseData = hashMapOf(
            "Category" to selectedCategory,
            "Date" to date,
            "ExpenseAmt" to amount
        )

        val userExpenseRef =
            db.collection("Expense").document(currentUserUid).collection(userName).document()

        userExpenseRef.set(expenseData)
            .addOnSuccessListener {
                Log.d("Firestore", "Expense data saved successfully")
                // Reset fields after successful insertion
                editAmount.text.clear()
                editDate.text.clear()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding expense data", e)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedCategoryImage =
                data?.getIntExtra("selectedCategoryImage", R.drawable.coin) ?: R.drawable.coin
            val selectedCategoryName = data?.getStringExtra("selectedCategoryName") ?: ""

            selectedImageView.setImageResource(selectedCategoryImage)
            selectedCategory = selectedCategoryName

            // Find the TextView for displaying the selected category name
            val selectedCategoryTextView: TextView = findViewById(R.id.selectedCategoryName)
            selectedCategoryTextView.text = selectedCategoryName
        }
    }





    companion object {
        const val REQUEST_CODE = 100
    }
}
