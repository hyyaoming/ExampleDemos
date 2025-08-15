package com.example.sampleview.log

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface ILogWriter {
    suspend fun writeLog(log: LogEntry)
    suspend fun flush()
}

class FileLogWriter(context: Context) : ILogWriter {
    private val buffer = mutableListOf<LogEntry>()
    private val mutex = Mutex()
    private val bufferLimit = 20
    private val logFile = File(context.filesDir, "app_logs.txt")

    override suspend fun writeLog(log: LogEntry) {
        mutex.withLock {
            buffer.add(log)
            if (buffer.size >= bufferLimit) {
                flush()
            }
        }
    }

    override suspend fun flush() {
        val toWrite = mutex.withLock {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        if (toWrite.isEmpty()) return
        withContext(Dispatchers.IO) {
            logFile.appendText(toWrite.joinToString(separator = "\n") {
                "${it.timestamp} [${it.level}] ${it.tag}: ${it.message}"
            } + "\n")
        }
    }
}

class DatabaseLogWriter(context: Context) : ILogWriter {
    private val db = LogDatabase.getInstance(context)
    private val buffer = mutableListOf<LogEntryEntity>()
    private val mutex = Mutex()
    private val bufferLimit = 20

    override suspend fun writeLog(log: LogEntry) {
        val entity = LogEntryEntity(
            timestamp = log.timestamp,
            level = log.level,
            tag = log.tag,
            message = log.message
        )
        mutex.withLock {
            buffer.add(entity)
            if (buffer.size >= bufferLimit) {
                flush()
            }
        }
    }

    override suspend fun flush() {
        val toInsert = mutex.withLock {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        if (toInsert.isEmpty()) return
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.logDao().insertLogs(toInsert)
            }
        }
    }
}
