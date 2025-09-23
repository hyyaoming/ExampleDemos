package com.example.sampleview.eventtracker.logger

import com.example.sampleview.AppLogger

/**
 * 事件追踪日志接口，用于打印模块内部日志。
 * 可自定义实现替换默认日志输出。
 */
interface TrackerLogger {

    /** 打印日志 */
    fun log(message: String)

    companion object {
        /** 全局日志实现，默认使用 AppLogger */
        var logger: TrackerLogger = DefaultLogger()

        /** 默认日志实现，打印到 AppLogger */
        private class DefaultLogger : TrackerLogger {
            override fun log(message: String) {
                AppLogger.d("EventTracker", message)
            }
        }
    }
}
