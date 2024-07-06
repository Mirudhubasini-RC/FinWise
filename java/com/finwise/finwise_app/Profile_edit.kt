package com.finwise.finwise_app

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Profile_edit : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var toggleButton: ToggleButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usernameText: TextView
    private lateinit var emailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var totalSpendingTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private lateinit var passwordTextView: TextView
    private lateinit var passwordEditText: EditText
    private lateinit var editButton: Button
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        profileImage = findViewById(R.id.profile_image)
        toggleButton = findViewById(R.id.toggle_button)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        usernameText = findViewById(R.id.username_text)
        emailTextView = findViewById(R.id.email)
        logoutButton = findViewById(R.id.logout_button)
        totalSpendingTextView = findViewById(R.id.totalSpending)
        passwordTextView = findViewById(R.id.password_text)
        editButton = findViewById(R.id.edit_button)
        passwordEditText = findViewById(R.id.password_edit_text)
        editButton = findViewById(R.id.edit_button)

        // Load the profile image URI from SharedPreferences
        val profileImageUri = sharedPreferences.getString("PROFILE_IMAGE_URI", null)
        if (profileImageUri != null) {
            Glide.with(this).load(Uri.parse(profileImageUri)).into(profileImage)
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show options for selecting boy or girl image
                showImageOptionsDialog()
            }
        }

        // Retrieve username and email from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser
        val username = currentUser?.displayName
        val email = currentUser?.email

        // Display username and email in TextViews
        usernameText.text = username
        emailTextView.text = email

        // Fetch and display password
        fetchAndDisplayPassword()

        // Fetch and display total spending
        fetchAndDisplayTotalSpending()

        editButton.setOnClickListener {
            isEditing = !isEditing
            if (isEditing) {
                passwordTextView.visibility = View.GONE
                passwordEditText.visibility = View.VISIBLE
                passwordEditText.setText(passwordTextView.text)
            } else {
                passwordTextView.visibility = View.VISIBLE
                passwordEditText.visibility = View.GONE

                passwordTextView.text = passwordEditText.text

                // Save edited values to Firebase Database
                saveEditedValuesToDatabase()
            }
        }

        logoutButton.setOnClickListener {
            SessionManager.clearSession(this)

            // Navigate the user to the login screen
            val intent = Intent(this, Login::class.java)
            startActivity(intent)

            // Finish the current activity to prevent the user from navigating back to it using the back button
            this.finish()
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Finish current activity to navigate back
        }
    }

    private fun saveEditedValuesToDatabase() {
        // Get the current user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Update the password if it's changed
        val newPassword = passwordEditText.text.toString()
        if (newPassword.isNotEmpty()) {
            currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "User profile updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "New password is empty.",Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(imageUri: Uri) {
        Glide.with(this)
            .load(imageUri)
            .apply(RequestOptions.circleCropTransform())
            .into(profileImage)
    }

    private fun showImageOptionsDialog() {
        showToast("showImageOptionsDialog() function called")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image")

        val options = arrayOf("Boy", "Girl")

        builder.setItems(options) { _, which ->
            showToast("Option selected: $which")
            val selectedImageUri: String = if (which == 0) {
                showToast("Selected image: Boy")
                val resourceId = R.drawable.boy
                "android.resource://${packageName}/${resourceId}"
            } else {
                showToast("Selected image: Girl")
                val resourceId = R.drawable.beauty
                "android.resource://${packageName}/${resourceId}"
            }

            showToast("Selected image URI: $selectedImageUri")
            sharedPreferences.edit {
                putString("PROFILE_IMAGE_URI", selectedImageUri).apply()
            }
            loadProfileImage(Uri.parse(selectedImageUri))
        }
        builder.show()
    }

    private fun fetchAndDisplayPassword() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val password = currentUser?.providerData?.get(1)?.providerId

        password?.let {
            passwordTextView.text = it
        } ?: run {
            showToast("Password not available")
            passwordTextView.text = "Password not available"
        }
    }

    private fun fetchAndDisplayTotalSpending() {
        val currentUser = FirebaseAuth.getInstance().currentUser
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
                    val totalSpendingText = String.format(Locale.getDefault(), "%.2f", spending)
                    totalSpendingTextView.text = totalSpendingText
                } else {
                    showToast("Finance document for year $year, month $currentMonth does not exist")
                }
            }.addOnFailureListener { exception ->
                showToast("Error fetching total spending: ${exception.message}")
            }
        } else {
            showToast("Current user is null")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}