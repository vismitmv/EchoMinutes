package com.vismitmv.echominutes.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.vismitmv.echominutes.data.db.AppDatabase
import com.vismitmv.echominutes.data.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "EchoMinutesSyncWorker"
private const val UNIQUE_WORK_NAME = "EchoMinutesSyncWork"

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = SecurePrefs(applicationContext)
        val serverUrl = prefs.getServerUrl().trim().trimEnd('/')
        val syncKey = prefs.getSyncApiKey().trim()

        if (serverUrl.isBlank() || syncKey.isBlank()) {
            Log.d(TAG, "Sync skipped: server URL or Sync API key is not configured.")
            return@withContext Result.success()
        }

        val dao = AppDatabase.getInstance(applicationContext).meetingDao()
        val unsyncedMeetings = dao.getUnsyncedMeetings()

        if (unsyncedMeetings.isEmpty()) {
            Log.d(TAG, "No unsynced meetings found.")
            return@withContext Result.success()
        }

        var anyFailed = false

        for (meeting in unsyncedMeetings) {
            val audioFile = File(meeting.audioFilePath)
            if (!audioFile.exists()) {
                Log.w(TAG, "Audio file missing for meeting ID ${meeting.id}: ${meeting.audioFilePath}")
                // Mark as failed or skip
                dao.updateSyncStatus(meeting.id, "FAILED", null)
                continue
            }

            try {
                val mediaType = "audio/mp4".toMediaType()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", meeting.title)
                    .addFormDataPart("createdAt", meeting.createdAt.toString())
                    .addFormDataPart("durationSeconds", meeting.durationSeconds.toString())
                    .addFormDataPart("transcript", meeting.transcript)
                    .addFormDataPart("summary", meeting.summary)
                    .addFormDataPart(
                        "audio",
                        audioFile.name,
                        audioFile.asRequestBody(mediaType)
                    )
                    .build()

                val request = Request.Builder()
                    .url("$serverUrl/api/v1/sync")
                    .addHeader("Authorization", "Bearer $syncKey")
                    .addHeader("X-Sync-Key", syncKey)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    Log.i(TAG, "Meeting ${meeting.id} synced successfully. Server response: $responseBody")
                    dao.updateSyncStatus(meeting.id, "SYNCED", System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Meeting ${meeting.id} sync failed: HTTP ${response.code} $responseBody")
                    dao.updateSyncStatus(meeting.id, "FAILED", null)
                    anyFailed = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception syncing meeting ${meeting.id} to $serverUrl", e)
                dao.updateSyncStatus(meeting.id, "FAILED", null)
                anyFailed = true
            }
        }

        if (anyFailed) {
            Log.w(TAG, "One or more meetings failed to sync. Scheduling retry with exponential backoff...")
            Result.retry()
        } else {
            Log.i(TAG, "All meetings synced successfully.")
            Result.success()
        }
    }

    companion object {
        fun enqueue(context: Context, force: Boolean = false) {
            val prefs = SecurePrefs(context)
            if (!force && !prefs.isAutoSyncEnabled()) {
                Log.d(TAG, "Auto-sync is disabled in settings.")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
    }
}
