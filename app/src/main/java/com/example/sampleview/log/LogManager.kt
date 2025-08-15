package com.example.sampleview.log

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LogManager private constructor(
    private val writer: ILogWriter, private val filter: ILogFilter
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun log(level: String, tag: String, message: String) = apply {
        val logEntry = LogEntry(level = level, tag = tag, message = message)
        if (filter.shouldWrite(logEntry)) {
            coroutineScope.launch {
                writer.writeLog(logEntry)
            }
        }
    }

    fun d(tag: String, message: String) = apply { log("DEBUG", tag, message) }

    fun i(tag: String, message: String) = apply { log("INFO", tag, message) }

    fun w(tag: String, message: String) = apply { log("WARN", tag, message) }

    fun e(tag: String, message: String) = apply { log("ERROR", tag, message) }

    suspend fun flush() {
        writer.flush()
    }

    companion object {
        @Volatile
        private var instance: LogManager? = null

        fun get(): LogManager {
            return instance ?: throw IllegalStateException("LogManager is not initialized")
        }

        fun initialize(context: Context, block: LogManagerBuilder.() -> Unit = {}) {
            if (instance != null) return

            val builder = LogManagerBuilder(context).apply(block)
            instance = LogManager(builder.writer, builder.filter)
        }
    }

    class LogManagerBuilder(context: Context) {
        var writer: ILogWriter = FileLogWriter(context)
            private set
        var filter: ILogFilter = LevelFilter("DEBUG")
            private set

        fun writer(block: () -> ILogWriter) {
            writer = block()
        }

        fun filter(block: () -> ILogFilter) {
            filter = block()
        }
    }
}
