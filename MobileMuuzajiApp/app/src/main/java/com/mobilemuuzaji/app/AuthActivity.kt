package com.mobilemuuzaji.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobilemuuzaji.app.network.ApiClient
import com.mobilemuuzaji.app.network.models.LoginRequest
import com.mobilemuuzaji.app.network.models.SignupRequest
import com.google.gson.Gson
import com.mobilemuuzaji.app.network.models.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    // Tracks whether we are in signup or login mode
    private var isSignupMode = true

    private lateinit var tvAuthTitle:          TextView
    private lateinit var llErrors:             LinearLayout
    private lateinit var tvNameLabel:          TextView
    private lateinit var etName:               EditText
    private lateinit var etEmail:              EditText
    private lateinit var etPassword:           EditText
    private lateinit var tvRepeatPasswordLabel:TextView
    private lateinit var etRepeatPassword:     EditText
    private lateinit var cbTerms:              CheckBox
    private lateinit var btnSubmit:            Button
    private lateinit var tvToggleAuth:         TextView
    private lateinit var sessionManager:       SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        sessionManager = SessionManager(this)

        // Bind views
        tvAuthTitle           = findViewById(R.id.tvAuthTitle)
        llErrors              = findViewById(R.id.llErrors)
        tvNameLabel           = findViewById(R.id.tvNameLabel)
        etName                = findViewById(R.id.etName)
        etEmail               = findViewById(R.id.etEmail)
        etPassword            = findViewById(R.id.etPassword)
        tvRepeatPasswordLabel = findViewById(R.id.tvRepeatPasswordLabel)
        etRepeatPassword      = findViewById(R.id.etRepeatPassword)
        cbTerms               = findViewById(R.id.cbTerms)
        btnSubmit             = findViewById(R.id.btnSubmit)
        tvToggleAuth          = findViewById(R.id.tvToggleAuth)

        // Start in signup mode
        updateUIForMode()

        btnSubmit.setOnClickListener {
            clearErrors()
            if (isSignupMode) handleSignup() else handleLogin()
        }

        // Tapping the toggle link switches between signup and login
        tvToggleAuth.setOnClickListener {
            isSignupMode = !isSignupMode
            clearErrors()
            clearFields()
            updateUIForMode()
        }
    }

    private fun updateUIForMode() {
        if (isSignupMode) {
            tvAuthTitle.text      = "Sign Up"
            btnSubmit.text        = "Sign Up"
            tvToggleAuth.text     = "Already have an account? Log in"

            // Show signup-only fields
            tvNameLabel.visibility           = View.VISIBLE
            etName.visibility                = View.VISIBLE
            tvRepeatPasswordLabel.visibility = View.VISIBLE
            etRepeatPassword.visibility      = View.VISIBLE
            cbTerms.visibility               = View.VISIBLE
        } else {
            tvAuthTitle.text  = "Log In"
            btnSubmit.text    = "Log In"
            tvToggleAuth.text = "Don't have an account? Sign up"

            // Hide signup-only fields
            tvNameLabel.visibility           = View.GONE
            etName.visibility                = View.GONE
            tvRepeatPasswordLabel.visibility = View.GONE
            etRepeatPassword.visibility      = View.GONE
            cbTerms.visibility               = View.GONE
        }
    }

    private fun handleSignup() {
        val name           = etName.text.toString().trim()
        val email          = etEmail.text.toString().trim()
        val password       = etPassword.text.toString()
        val repeatPassword = etRepeatPassword.text.toString()

        // Client-side validation before hitting the API
        val errors = mutableListOf<String>()

        if (name.isEmpty())                        errors.add("Name is required")
        if (email.isEmpty())                       errors.add("Email is required")
        if (password.isEmpty())                    errors.add("Password is required")
        if (password != repeatPassword)            errors.add("Passwords do not match")
        if (password.length < 8)                   errors.add("Password must be at least 8 characters")
        if (!cbTerms.isChecked)                    errors.add("You must accept the Terms and Conditions")

        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }

        // All client-side checks passed — call the API
        lifecycleScope.launch {
            btnSubmit.isEnabled = false    // prevent double tapping

            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.signup(
                        SignupRequest(
                            name            = name,
                            email           = email,
                            password        = password,
                            password_repeat = repeatPassword
                        )
                    )
                }

                if (response.isSuccessful) {
                    val authResponse = response.body()!!
                    // Save session locally
                    sessionManager.saveSession(
                        userId = authResponse.user.id,
                        name   = authResponse.user.name,
                        email  = authResponse.user.email,
                        adminOrgs    = authResponse.user.admin_orgs,
                        employeeOrgs = authResponse.user.employee_orgs
                    )
                    navigateAfterAuth()
                } else {
                    // Parse error response from API
                    val errorBody = response.errorBody()?.string()
                    handleApiError(errorBody)
                }

            } catch (e: Exception) {
                showErrors(listOf("Network error: ${e.message}. Try again!"))
            }

            btnSubmit.isEnabled = true
        }
    }

    private fun handleLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        val errors = mutableListOf<String>()

        if (email.isEmpty())    errors.add("Email is required")
        if (password.isEmpty()) errors.add("Password is required")

        if (errors.isNotEmpty()) {
            showErrors(errors)
            return
        }

        lifecycleScope.launch {
            btnSubmit.isEnabled = false

            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.login(
                        LoginRequest(email = email, password = password)
                    )
                }

                if (response.isSuccessful) {
                    val authResponse = response.body()!!
                    sessionManager.saveSession(
                        userId = authResponse.user.id,
                        name   = authResponse.user.name,
                        email  = authResponse.user.email,
                        adminOrgs    = authResponse.user.admin_orgs,
                        employeeOrgs = authResponse.user.employee_orgs
                    )
                    navigateAfterAuth()
                } else {
                    val errorBody = response.errorBody()?.string()
                    handleApiError(errorBody)
                }

            } catch (e: Exception) {
                showErrors(listOf("Network error: ${e.message}"))
            }

            btnSubmit.isEnabled = true
        }
    }

    private fun navigateAfterAuth() {
        val intent = Intent(this, OrganizationsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleApiError(errorBody: String?) {
        if (errorBody == null) {
            showErrors(listOf("An unexpected error occurred"))
            return
        }

        try {
            // Try to parse as a Pydantic validation error list
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

            if (errorResponse.detail != null) {
                when (errorResponse.detail) {
                    is String -> {
                        // 401 style: { "detail": "Invalid credentials" }
                        showErrors(listOf(errorResponse.detail))
                    }
                    is List<*> -> {
                        // Pydantic style: { "detail": [{ "loc": [...], "msg": "..." }] }
                        val messages = (errorResponse.detail as List<*>).map { item ->
                            val map = item as? Map<*, *>
                            val loc = (map?.get("loc") as? List<*>)?.lastOrNull() ?: "field"
                            val msg = map?.get("msg") ?: "Unknown error"
                            "$loc: $msg"
                        }
                        showErrors(messages)
                    }
                    else -> showErrors(listOf("An unexpected error occurred"))
                }
            } else if (errorResponse.message != null) {
                showErrors(listOf(errorResponse.message))
            } else {
                showErrors(listOf("An unexpected error occurred"))
            }
        } catch (e: Exception) {
            showErrors(listOf("An unexpected error occurred"))
        }
    }

    private fun showErrors(errors: List<String>) {
        llErrors.removeAllViews()     // clear any previous errors
        llErrors.visibility = View.VISIBLE

        errors.forEach { message ->
            // Inflate a new error box for each error
            val errorView = layoutInflater.inflate(R.layout.item_error, llErrors, false)
            errorView.findViewById<TextView>(R.id.tvError).text = message
            llErrors.addView(errorView)
        }

        // Scroll to top so the user sees the errors
        (llErrors.parent.parent as? ScrollView)?.smoothScrollTo(0, 0)
    }

    private fun clearErrors() {
        llErrors.removeAllViews()
        llErrors.visibility = View.GONE
    }

    private fun clearFields() {
        etName.text.clear()
        etEmail.text.clear()
        etPassword.text.clear()
        etRepeatPassword.text.clear()
        cbTerms.isChecked = false
    }
}