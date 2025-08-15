package com.example.sampleview.log

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DatabaseLogSource(context: Context) : LogSource {

    private val dao = LogDatabase.getInstance(context).logDao()

    override suspend fun writeLog(log: LogEntry) = withContext(Dispatchers.IO) {
        dao.insertLogs(listOf(log.toEntity()))
    }

    override suspend fun writeLogs(logs: List<LogEntry>) = withContext(Dispatchers.IO) {
        dao.insertLogs(logs.map { it.toEntity() })
    }

    override suspend fun getLogs(limit: Int): List<LogEntry> = withContext(Dispatchers.IO) {
        dao.getLogs(limit).map { it.toLogEntry() }
    }

    override suspend fun onLogsUploaded(maxTimestamp: Long) = withContext(Dispatchers.IO) {
        dao.deleteLogsBefore(maxTimestamp)
    }

    private fun LogEntry.toEntity(): LogEntryEntity {
        return LogEntryEntity(
            timestamp = this.timestamp, level = this.level, tag = this.tag, message = this.message
        )
    }

    private fun LogEntryEntity.toLogEntry(): LogEntry {
        return LogEntry(
            timestamp = this.timestamp, level = this.level, tag = this.tag, message = this.message
        )
    }
}

class FileLogSource(
    private val context: Context, private val logFileName: String = "logs.txt"
) : LogSource {

    private val logFile: File
        get() = File(context.filesDir, logFileName)

    override suspend fun writeLog(log: LogEntry) = withContext(Dispatchers.IO) {
        logFile.appendText("${log.timestamp}|${log.level}|${log.tag}|${log.message}\n")
    }

    override suspend fun writeLogs(logs: List<LogEntry>) = withContext(Dispatchers.IO) {
        logFile.appendText(logs.joinToString(separator = "\n") {
            "${it.timestamp}|${it.level}|${it.tag}|${it.message}"
        } + "\n")
    }

    override suspend fun getLogs(limit: Int): List<LogEntry> = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext emptyList()

        logFile.useLines { lines ->
            lines.take(limit).mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size == 4) {
                    val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val level = parts[1]
                    val tag = parts[2]
                    val message = parts[3]
                    LogEntry(timestamp, level, tag, message)
                } else null
            }.toList()
        }
    }

    override suspend fun onLogsUploaded(maxTimestamp: Long) = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext

        val remaining = logFile.readLines().filter { line ->
            val parts = line.split("|", limit = 4)
            val ts = parts.getOrNull(0)?.toLongOrNull() ?: return@filter true
            ts > maxTimestamp
        }

        logFile.writeText(remaining.joinToString("\n") + "\n")
    }
}

class MockLogUploader : LogUploader {
    override suspend fun uploadLogs(logs: List<LogEntry>): Boolean {
        // 模拟上传耗时
        kotlinx.coroutines.delay(1000)
        println("MockLogUploader: uploaded ${logs.size} logs")
        return true
    }
}

class RealLogUploader() : LogUploader {
    override suspend fun uploadLogs(logs: List<LogEntry>): Boolean {
        return try {
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


