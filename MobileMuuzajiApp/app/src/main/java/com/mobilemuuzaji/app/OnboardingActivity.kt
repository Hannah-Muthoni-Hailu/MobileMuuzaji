package com.mobilemuuzaji.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    // Tracks which screen the user is currently on
    // 0 = first screen, 1 = second screen
    private var currentPage = 0

    // Holds the data for each onboarding screen
    // Each entry is a Triple of (image resource, title, description)
    private val pages = listOf(
        Triple(
            R.drawable.onboarding_one,
            "Track Your Inventory",
            "Keep track of your stock levels and never run out of essential items again."
        ),
        Triple(
            R.drawable.onboarding_two,
            "Record Your Sales",
            "Log every sale instantly, even when you're offline. Sync when you're back online."
        )
    )

    private lateinit var ivOnboardingImage: ImageView
    private lateinit var tvOnboardingTitle: TextView
    private lateinit var tvOnboardingDescription: TextView
    private lateinit var btnSkip: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Bind views
        ivOnboardingImage       = findViewById(R.id.ivOnboardingImage)
        tvOnboardingTitle       = findViewById(R.id.tvOnboardingTitle)
        tvOnboardingDescription = findViewById(R.id.tvOnboardingDescription)
        btnSkip                 = findViewById(R.id.btnSkip)
        btnNext                 = findViewById(R.id.btnNext)

        // Load the first page
        loadPage(currentPage)

        // Skip always goes straight to signup
        btnSkip.setOnClickListener {
            navigateToSignup()
        }

        btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) {
                // Not on the last page yet — move to the next one
                currentPage++
                loadPage(currentPage)
            } else {
                // On the last page — go to signup
                navigateToSignup()
            }
        }
    }

    private fun loadPage(index: Int) {
        val (image, title, description) = pages[index]

        // Update the UI with the current page's content
        ivOnboardingImage.setImageResource(image)
        tvOnboardingTitle.text       = title
        tvOnboardingDescription.text = description

        // Change the Next button label to "Get Started" on the last page
        // so the user knows they're about to leave onboarding
        btnNext.text = if (index == pages.size - 1) "Get Started" else "Next"
    }

    private fun navigateToSignup() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", true).apply()

        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}