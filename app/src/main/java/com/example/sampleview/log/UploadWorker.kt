package com.example.sampleview.log

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UploadWorker(
    context: Context, params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val logSource: LogSource = DatabaseLogSource(context)
    private val logUploader: LogUploader = MockLogUploader()

    override suspend fun doWork(): Result {
        val logs = logSource.getLogs(100)
        if (logs.isEmpty()) return Result.success()
        val success = logUploader.uploadLogs(logs)
        if (success) {
            val maxTimestamp = logs.maxOf { it.timestamp }
            logSource.onLogsUploaded(maxTimestamp)
            return Result.success()
        }
        return Result.retry()
    }
}
