package com.streamlux.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
object VidVaultLauncher {

    fun open(context: Context, url: String, title: String = "VidVault") {
        try {
            val uri = Uri.parse(url)
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(0xFF1A1A1A.toInt())
                        .setNavigationBarColor(0xFF000000.toInt())
                        .build()
                )

            val customTabsIntent = builder.build()
            val activity = context.findActivity()
            if (activity != null) {
                customTabsIntent.launchUrl(activity, uri)
            } else {
                openExternal(context, uri)
            }
            Toast.makeText(
                context,
                "VidVault opened — use download links on the page",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("VidVaultLauncher", "Custom Tabs failed: ${e.message}")
            try {
                openExternal(context, Uri.parse(url))
            } catch (ex: Exception) {
                Log.e("VidVaultLauncher", "External browser failed: ${ex.message}")
                Toast.makeText(context, "Could not open VidVault. Try again later.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openExternal(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
