package com.finwise.finwise_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler

class Splash : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 1500
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        Handler().postDelayed({
            // Start main activity after SPLASH_DELAY
            val intent = Intent(this@Splash, Login::class.java)
            startActivity(intent)
            finish() // Close splash activity
        }, SPLASH_DELAY)
    }
}