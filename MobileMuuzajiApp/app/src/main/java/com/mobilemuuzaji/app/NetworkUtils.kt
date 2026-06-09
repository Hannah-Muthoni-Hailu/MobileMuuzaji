package com.mobilemuuzaji.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    fun isOnline(context: Context): Boolean {

        // ConnectivityManager is Android's system service for network information
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        // getActiveNetwork() returns the network the device is currently using
        // returns null if there is no active network at all
        val activeNetwork = connectivityManager.activeNetwork
            ?: return false    // no active network — definitely offline

        // NetworkCapabilities describes what the active network can do
        // returns null if the network exists but has no capabilities yet
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return false    // no capabilities — treat as offline

        // Check if the network has actual internet transport
        // TRANSPORT_WIFI     — connected via WiFi
        // TRANSPORT_CELLULAR — connected via mobile data
        // TRANSPORT_ETHERNET — connected via ethernet cable
        // We check all three so the app works regardless of connection type
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}