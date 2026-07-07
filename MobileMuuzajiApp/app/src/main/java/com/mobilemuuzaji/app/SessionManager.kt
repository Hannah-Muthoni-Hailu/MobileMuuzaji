package com.mobilemuuzaji.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilemuuzaji.app.network.models.OrganizationData

class SessionManager(context: Context) {

    // SharedPreferences is Android's simple key-value store
    // MODE_PRIVATE means only this app can read these values
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME    = "user_name"
        const val KEY_EMAIL   = "user_email"
        const val KEY_ADMIN_ORGS   = "admin_orgs"
        const val KEY_EMPLOYEE_ORGS = "employee_orgs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Save user details after successful login or signup
    fun saveSession(userId: Int, name: String, email: String, adminOrgs: List<OrganizationData>, employeeOrgs: List<OrganizationData>) {
        prefs.edit()
            .putInt(KEY_USER_ID,    userId)
            .putString(KEY_NAME,    name)
            .putString(KEY_EMAIL,   email)
            .putString(KEY_ADMIN_ORGS,    Gson().toJson(adminOrgs))
            .putString(KEY_EMPLOYEE_ORGS, Gson().toJson(employeeOrgs))
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()    // apply() saves asynchronously — never use commit() on the main thread
    }

    fun getUserId(): Int     = prefs.getInt(KEY_USER_ID,   -1)
    fun getName():   String? = prefs.getString(KEY_NAME,   null)
    fun getEmail():  String? = prefs.getString(KEY_EMAIL,  null)

    fun getAdminOrgs(): List<OrganizationData> {
        val json = prefs.getString(KEY_ADMIN_ORGS, null) ?: return emptyList()
        val type = object : TypeToken<List<OrganizationData>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getEmployeeOrgs(): List<OrganizationData> {
        val json = prefs.getString(KEY_EMPLOYEE_ORGS, null) ?: return emptyList()
        val type = object : TypeToken<List<OrganizationData>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != -1

    // Clear everything on logout
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}