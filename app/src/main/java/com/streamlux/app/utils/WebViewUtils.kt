package com.streamlux.app.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.FileOutputStream

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class BlobDownloadInterface(private val context: Context) {
    @JavascriptInterface
    fun downloadBlob(base64Data: String, fileName: String, mimeType: String) {
        try {
            val fileData = Base64.decode(base64Data.substringAfter("base64,"), Base64.DEFAULT)
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            FileOutputStream(file).use { it.write(fileData) }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Blob Download Complete: $fileName", Toast.LENGTH_LONG).show()
            }
            
            // Trigger a scan so it shows up in file manager immediately
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        } catch (e: Exception) {
            Log.e("StreamLuxJS", "Blob download failed", e)
        }
    }
}

object BrowserConstants {
    const val DESKTOP_CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    val MIRROR_DOMAINS = listOf(
        "vidvault", "videasy", "vidsrc", "vidplay", "2embed", "dl.", "storage"
    )
}
