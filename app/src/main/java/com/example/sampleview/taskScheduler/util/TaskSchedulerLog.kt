package com.example.sampleview.taskScheduler.util

import com.example.sampleview.taskScheduler.api.TaskLogger
import com.example.sampleview.taskScheduler.impl.DefaultTaskLogger

object TaskSchedulerLog {
    var logger: TaskLogger = DefaultTaskLogger()
    private const val TAG = "TaskScheduler"

    enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    private fun getThreadInfo(): String {
        return " [运行线程:${Thread.currentThread().name}]"
    }

    /**
     * 通用日志打印方法
     * @param level 日志级别
     * @param msg 日志内容
     * @param throwable 异常，可选，仅ERROR级别使用
     */
    fun log(level: LogLevel, msg: String, throwable: Throwable? = null) {
        val msgWithThread = msg + getThreadInfo()
        when (level) {
            LogLevel.DEBUG -> logger.d(TAG, msgWithThread)
            LogLevel.INFO -> logger.i(TAG, msgWithThread)
            LogLevel.WARN -> logger.w(TAG, msgWithThread)
            LogLevel.ERROR -> logger.e(TAG, msgWithThread, throwable)
        }
    }

    fun d(msg: String) = log(LogLevel.DEBUG, msg)

    fun i(msg: String) = log(LogLevel.INFO, msg)

    fun w(msg: String) = log(LogLevel.WARN, msg)

    fun e(msg: String, throwable: Throwable? = null) = log(LogLevel.ERROR, msg, throwable)
}