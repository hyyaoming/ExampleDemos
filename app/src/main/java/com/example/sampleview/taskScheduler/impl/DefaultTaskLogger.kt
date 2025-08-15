package com.example.sampleview.taskScheduler.impl

import android.util.Log
import com.example.sampleview.taskScheduler.api.TaskLogger

/**
 * 默认日志实现，基于 Android 原生 Log 类。
 *
 * 该实现简单直接，将日志输出到 Android 日志系统，
 * 方便在开发调试和发布环境查看日志信息。
 *
 * 继承自 [TaskLogger] 接口，覆盖了所有日志级别的方法。
 */
class DefaultTaskLogger : TaskLogger {

    /** 输出调试日志，调用 Android Log.d */
    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    /** 输出信息日志，调用 Android Log.i */
    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    /** 输出警告日志，调用 Android Log.w */
    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    /**
     * 输出错误日志，调用 Android Log.e。
     * 如果传入异常对象，则附带异常堆栈，否则只输出错误消息。
     */
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, msg, throwable)
        else Log.e(tag, msg)
    }
}
