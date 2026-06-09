package com.mobilemuuzaji.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvMessage: TextView
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views to their XML counterparts using their IDs
        tvMessage = findViewById(R.id.tvMessage)
        btnRetry  = findViewById(R.id.btnRetry)

        // Run the connectivity check when the activity first loads
        checkConnectivityAndProceed()

        // When the user taps Retry, run the check again
        btnRetry.setOnClickListener {
            checkConnectivityAndProceed()
        }
    }

    private fun checkConnectivityAndProceed() {
        if (NetworkUtils.isOnline(this)) {
            // User is online — proceed to onboarding
            // For now we just show a message; later this will navigate to onboarding
            showOnlineState()
        } else {
            // User is offline — show the offline message
            showOfflineState()
        }
    }

    private fun showOnlineState() {
        // Hide the retry button since it's not needed
        btnRetry.visibility = View.GONE

        // Show a brief message while we navigate away
        tvMessage.text = "Connected! Loading..."

        // Navigate to the onboarding screen
        // We will replace this with the actual onboarding Activity later
        // For now this confirms the check is working
        navigateToOnboarding()
    }

    private fun showOfflineState() {
        // Show the retry button so the user can try again
        btnRetry.visibility = View.VISIBLE

        // Tell the user what's wrong and what to do
        tvMessage.text = "No internet connection.\nPlease go online to use MobileMuuzaji."
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // onResume fires every time the activity becomes visible
        // including when the user returns from switching on their WiFi
        checkConnectivityAndProceed()
    }
}