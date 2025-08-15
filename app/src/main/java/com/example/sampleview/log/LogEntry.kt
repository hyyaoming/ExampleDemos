package com.example.sampleview.log

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
) {
    fun toLogLine(): String = "$timestamp|$level|$tag|$message"

    companion object {
        fun fromLogLine(line: String): LogEntry? {
            val parts = line.split("|")
            if (parts.size < 4) return null
            val ts = parts[0].toLongOrNull() ?: return null
            return LogEntry(ts, parts[1], parts[2], parts[3])
        }
    }
}
