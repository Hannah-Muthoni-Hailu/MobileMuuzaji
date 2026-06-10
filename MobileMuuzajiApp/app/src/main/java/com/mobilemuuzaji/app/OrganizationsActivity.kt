package com.mobilemuuzaji.app

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobilemuuzaji.app.network.ApiClient
import com.mobilemuuzaji.app.network.models.NewOrgRequest
import com.mobilemuuzaji.app.network.models.OrgListItem
import com.google.gson.Gson
import com.mobilemuuzaji.app.network.models.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobilemuuzaji.app.network.models.OrganizationData

class OrganizationsActivity : AppCompatActivity() {

    private lateinit var btnNewOrg:        Button
    private lateinit var tvSuccessMessage: TextView
    private lateinit var tvEmptyState:     TextView
    private lateinit var lvOrganizations:  ListView
    private lateinit var sessionManager:   SessionManager

    // Mutable list so we can add to it after creating a new org
    private val organizations = mutableListOf<OrgListItem>()
    private lateinit var adapter: OrganizationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizations)

        sessionManager   = SessionManager(this)
        btnNewOrg        = findViewById(R.id.btnNewOrg)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)
        tvEmptyState     = findViewById(R.id.tvEmptyState)
        lvOrganizations  = findViewById(R.id.lvOrganizations)

        adapter = OrganizationsAdapter(this, organizations)
        lvOrganizations.adapter = adapter

        // Load organizations from the session data saved on login
        loadOrganizationsFromSession()

        // Show empty state if no organizations
        updateEmptyState()

        btnNewOrg.setOnClickListener {
            showNewOrgDialog()
        }

        // Navigate to organization page when an org is tapped
        lvOrganizations.setOnItemClickListener { _, _, position, _ ->
            val org = organizations[position]
            val intent = Intent(this, OrganizationActivity::class.java)
            intent.putExtra("org_id",   org.id)
            intent.putExtra("org_name", org.name)
            intent.putExtra("org_role", org.role)
            startActivity(intent)
        }
    }

    private fun loadOrganizationsFromSession() {
        // Build the organization list from data saved during login/signup
        // Admin organizations
        sessionManager.getAdminOrgs().forEach { org ->
            organizations.add(OrgListItem(id = org.id, name = org.org_name, role = "Admin"))
        }

        // Employee organizations
        sessionManager.getEmployeeOrgs().forEach { org ->
            organizations.add(OrgListItem(id = org.id, name = org.org_name, role = "Employee"))
        }

        adapter.notifyDataSetChanged()
    }

    private fun showNewOrgDialog() {
        // Dialog is Android's built-in modal window
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_org)

        // Make the dialog width match the screen
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val llDialogErrors  = dialog.findViewById<LinearLayout>(R.id.llDialogErrors)
        val etOrgName       = dialog.findViewById<EditText>(R.id.etOrgName)
        val btnDialogCancel = dialog.findViewById<Button>(R.id.btnDialogCancel)
        val btnDialogSubmit = dialog.findViewById<Button>(R.id.btnDialogSubmit)

        btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDialogSubmit.setOnClickListener {
            val orgName = etOrgName.text.toString().trim()

            // Clear previous errors
            llDialogErrors.removeAllViews()
            llDialogErrors.visibility = View.GONE

            if (orgName.isEmpty()) {
                showDialogError(llDialogErrors, "Organization name is required")
                return@setOnClickListener
            }

            btnDialogSubmit.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.apiService.createOrganization(
                            NewOrgRequest(
                                org_name = orgName,
                                admin_id  = sessionManager.getUserId()
                            )
                        )
                    }

                    if (response.isSuccessful) {
                        val authResponse = response.body()!!

                        // Update session with new org data
                        sessionManager.saveSession(
                            userId = authResponse.user.id,
                            name   = authResponse.user.name,
                            email  = authResponse.user.email,
                            adminOrgs    = authResponse.user.admin_orgs,
                            employeeOrgs = authResponse.user.employee_orgs
                        )

                        // Add the new org to the list
                        val newOrg = authResponse.user.admin_orgs.lastOrNull()
                        if (newOrg != null) {
                            organizations.add(
                                OrgListItem(
                                    id   = newOrg.id,
                                    name = newOrg.org_name,
                                    role = "Admin"
                                )
                            )
                            adapter.notifyDataSetChanged()
                            updateEmptyState()
                        }

                        dialog.dismiss()
                        showSuccessMessage("Organization '${orgName}' created successfully")

                    } else {
                        val errorBody = response.errorBody()?.string()
                        val message   = parseErrorMessage(errorBody)
                        showDialogError(llDialogErrors, message)
                    }

                } catch (e: Exception) {
                    showDialogError(llDialogErrors, "Network error: ${e.message}")
                }

                btnDialogSubmit.isEnabled = true
            }
        }

        dialog.show()
    }

    private fun showSuccessMessage(message: String) {
        tvSuccessMessage.text       = message
        tvSuccessMessage.visibility = View.VISIBLE

        // Auto-hide after 4 seconds
        tvSuccessMessage.postDelayed({
            tvSuccessMessage.visibility = View.GONE
        }, 4000)
    }

    private fun showDialogError(container: LinearLayout, message: String) {
        container.removeAllViews()
        val errorView = layoutInflater.inflate(R.layout.item_error, container, false)
        errorView.findViewById<TextView>(R.id.tvError).text = message
        container.addView(errorView)
        container.visibility = View.VISIBLE
    }

    private fun updateEmptyState() {
        tvEmptyState.visibility = if (organizations.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "An unexpected error occurred"
        return try {
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            when (val detail = errorResponse.detail) {
                is String   -> detail
                is List<*>  -> {
                    val map  = (detail.firstOrNull()) as? Map<*, *>
                    val loc  = (map?.get("loc") as? List<*>)?.lastOrNull() ?: "field"
                    val msg  = map?.get("msg") ?: "Unknown error"
                    "$loc: $msg"
                }
                else -> "An unexpected error occurred"
            }
        } catch (e: Exception) {
            "An unexpected error occurred"
        }
    }
}