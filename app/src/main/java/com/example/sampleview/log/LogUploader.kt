package com.example.sampleview.log

/**
 * 日志上传器接口，定义上传日志的方法
 */
interface LogUploader {
    /**
     * 上传日志列表
     * @return 上传是否成功
     */
    suspend fun uploadLogs(logs: List<LogEntry>): Boolean
}

interface LogSource {

    /**
     * 写入单条日志
     */
    suspend fun writeLog(log: LogEntry)

    /**
     * 写入多条日志（可选优化）
     */
    suspend fun writeLogs(logs: List<LogEntry>) {
        logs.forEach { writeLog(it) }
    }

    /**
     * 获取日志列表
     */
    suspend fun getLogs(limit: Int): List<LogEntry>

    /**
     * 上传成功后的回调（做清理或标记）
     */
    suspend fun onLogsUploaded(maxTimestamp: Long)
}
