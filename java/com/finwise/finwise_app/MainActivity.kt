package com.finwise.finwise_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var userNameEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var nameErrorTextView: TextView
    private lateinit var emailErrorTextView: TextView
    private lateinit var passwordErrorTextView: TextView
    private lateinit var confirmPasswordErrorTextView: TextView
    private lateinit var alreadyHaveAccountTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var phoneNumberErrorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        userNameEditText = findViewById(R.id.userNameEditText)
        signUpButton = findViewById(R.id.signUpButton)
        nameErrorTextView = findViewById(R.id.nameError)
        emailErrorTextView = findViewById(R.id.emailError)
        passwordErrorTextView = findViewById(R.id.passwordError)
        confirmPasswordErrorTextView = findViewById(R.id.passwordMismatchError)
        alreadyHaveAccountTextView = findViewById(R.id.alreadyHaveAccountText)
        phoneNumberErrorTextView = findViewById(R.id.phoneNumberError)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            val userName = userNameEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()

            if (validateInput(email, password, confirmPassword, userName, phoneNumber)) {
                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = mAuth.currentUser
                            user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    // Update user profile with display name
                                    user.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(userName)
                                            .build()
                                    ).addOnCompleteListener { updateProfileTask ->
                                        if (updateProfileTask.isSuccessful) {
                                            Toast.makeText(
                                                this,
                                                "Sign up successful! Verification email sent.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Navigate to the login activity
                                            val intent = Intent(this, Login::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Failed to send verification email. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Sign up failed. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

        alreadyHaveAccountTextView.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInput(
        email: String,
        password: String,
        confirmPassword: String,
        userName: String,
        phoneNumber: String
    ): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$".toRegex()

        if (phoneNumber.isEmpty() || !Patterns.PHONE.matcher(phoneNumber).matches()) {
            phoneNumberErrorTextView.text = "Enter a valid phone number"
            phoneNumberErrorTextView.visibility = View.VISIBLE
            phoneNumberErrorTextView.setTextColor(Color.RED)
            return false
        } else {
            phoneNumberErrorTextView.visibility = View.GONE
        }

        if (userName.isEmpty()) {
            nameErrorTextView.text = "Please enter your name"
            nameErrorTextView.visibility = View.VISIBLE
            nameErrorTextView.setTextColor(Color.RED)
            return false
        } else {
            nameErrorTextView.visibility = View.GONE
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErrorTextView.text = "Enter a valid email address"
            emailErrorTextView.visibility = View.VISIBLE
            emailErrorTextView.setTextColor(Color.RED)
            return false
        } else {
            emailErrorTextView.visibility = View.GONE
        }

        if (password.isEmpty() || password.length < 6 || !password.matches(passwordPattern)) {
            passwordErrorTextView.text =
                "Password must be at least 6 characters long and contain at least one digit, one capital letter, and one special character"
            passwordErrorTextView.visibility = View.VISIBLE
            passwordErrorTextView.setTextColor(Color.RED)
            return false
        } else {
            passwordErrorTextView.visibility = View.GONE
        }

        if (confirmPassword != password) {
            confirmPasswordErrorTextView.text = "Passwords do not match"
            confirmPasswordErrorTextView.visibility = View.VISIBLE
            confirmPasswordErrorTextView.setTextColor(Color.RED)
            return false
        } else {
            confirmPasswordErrorTextView.visibility = View.GONE
        }

        return true
    }
}
