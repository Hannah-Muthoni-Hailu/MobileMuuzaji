package com.mobilemuuzaji.app

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.mobilemuuzaji.app.network.ApiClient
import com.mobilemuuzaji.app.network.models.EmployeeData
import com.mobilemuuzaji.app.network.models.EmployeeOrgResponse
import com.mobilemuuzaji.app.network.models.ErrorResponse
import com.mobilemuuzaji.app.network.models.NewEmployeeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmployeesActivity : AppCompatActivity() {

    private lateinit var btnNewEmployee:  Button
    private lateinit var tvOfflineWarning:TextView
    private lateinit var progressBar:     ProgressBar
    private lateinit var tvError:         TextView
    private lateinit var tvSuccess:       TextView
    private lateinit var tvEmptyState:    TextView
    private lateinit var lvEmployees:     ListView
    private lateinit var sessionManager:  SessionManager

    private var orgId      = -1
    private var orgName    = ""
    private var employees  = mutableListOf<EmployeeData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employees)

        orgId   = intent.getIntExtra("org_id",   -1)
        orgName = intent.getStringExtra("org_name") ?: "Organization"

        sessionManager   = SessionManager(this)
        btnNewEmployee   = findViewById(R.id.btnNewEmployee)
        tvOfflineWarning = findViewById(R.id.tvOfflineWarning)
        progressBar      = findViewById(R.id.progressBar)
        tvError          = findViewById(R.id.tvError)
        tvSuccess        = findViewById(R.id.tvSuccess)
        tvEmptyState     = findViewById(R.id.tvEmptyState)
        lvEmployees      = findViewById(R.id.lvEmployees)

        // Load initial employee list passed from OrganizationActivity
        val initialEmployees = intent.getSerializableExtra("employees")
        if (initialEmployees != null) {
            @Suppress("UNCHECKED_CAST")
            employees.addAll(initialEmployees as ArrayList<EmployeeData>)
        }

        updateList()

        // Show offline warning and disable button if offline
        if (!NetworkUtils.isOnline(this)) {
            tvOfflineWarning.visibility = View.VISIBLE
            btnNewEmployee.isEnabled    = false
            btnNewEmployee.alpha        = 0.5f
        }

        btnNewEmployee.setOnClickListener {
            showNewEmployeeDialog()
        }
    }

    private fun showNewEmployeeDialog() {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(
                this,
                "Employee management requires an internet connection",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_employee)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val llErrors  = dialog.findViewById<LinearLayout>(R.id.llNewEmployeeErrors)
        val etEmail   = dialog.findViewById<EditText>(R.id.etEmployeeEmail)
        val btnCancel = dialog.findViewById<Button>(R.id.btnNewEmployeeCancel)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnNewEmployeeSubmit)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()

            llErrors.removeAllViews()
            llErrors.visibility = View.GONE

            // Validation
            if (email.isEmpty()) {
                showDialogError(llErrors, "Email is required")
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text      = "Adding..."

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.apiService.addEmployee(
                            NewEmployeeRequest(
                                org_id         = orgId,
                                employee_email = email
                            )
                        )
                    }

                    if (response.isSuccessful) {
                        val orgResponse = response.body()!!

                        // Replace employee list with updated list from server
                        employees.clear()
                        employees.addAll(orgResponse.employees)

                        updateList()
                        dialog.dismiss()
                        showSuccess("Employee added successfully")

                    } else {
                        val errorBody = response.errorBody()?.string()
                        showDialogError(llErrors, parseErrorMessage(errorBody))
                    }

                } catch (e: Exception) {
                    showDialogError(llErrors, "Network error: ${e.message}")
                }

                btnSubmit.isEnabled = true
                btnSubmit.text      = "Add Employee"
            }
        }

        dialog.show()
    }

    private fun updateList() {
        lvEmployees.adapter = EmployeeAdapter(
            context    = this,
            employees  = employees,
            onRemoveClick = { employee ->
                // TODO: implement remove employee
            }
        )
        tvEmptyState.visibility = if (employees.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSuccess(message: String) {
        tvSuccess.text       = message
        tvSuccess.visibility = View.VISIBLE
        tvSuccess.postDelayed({ tvSuccess.visibility = View.GONE }, 4000)
    }

    private fun showDialogError(container: LinearLayout, message: String) {
        container.removeAllViews()
        val errorView = layoutInflater.inflate(R.layout.item_error, container, false)
        errorView.findViewById<TextView>(R.id.tvError).text = message
        container.addView(errorView)
        container.visibility = View.VISIBLE
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "An unexpected error occurred"
        return try {
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            when (val detail = errorResponse.detail) {
                is String  -> detail
                is List<*> -> {
                    val map = detail.firstOrNull() as? Map<*, *>
                    val loc = (map?.get("loc") as? List<*>)?.lastOrNull() ?: "field"
                    val msg = map?.get("msg") ?: "Unknown error"
                    "$loc: $msg"
                }
                else -> "An unexpected error occurred"
            }
        } catch (e: Exception) {
            "An unexpected error occurred"
        }
    }
}