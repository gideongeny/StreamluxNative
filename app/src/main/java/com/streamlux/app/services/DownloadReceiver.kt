package com.streamlux.app.services

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.streamlux.app.data.local.LibraryDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var libraryDao: LibraryDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == -1L) return

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            scope.launch {
                try {
                    val item = libraryDao.getItemByDownloadId(downloadId)
                    if (item != null) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = dm.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIndex)
                            
                            val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = cursor.getString(localUriIndex)

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                libraryDao.insertItem(
                                    item.copy(
                                        downloadStatus = "completed",
                                        downloadProgress = 100,
                                        localUri = localUri
                                    )
                                )
                                Log.d("DownloadReceiver", "Download completed: ${item.title}")
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                libraryDao.insertItem(item.copy(downloadStatus = "failed"))
                                Log.d("DownloadReceiver", "Download failed: ${item.title}")
                            }
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    Log.e("DownloadReceiver", "Error processing completion", e)
                }
            }
        }
    }
}
