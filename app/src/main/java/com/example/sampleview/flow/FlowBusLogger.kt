package com.example.sampleview.flow

import android.util.Log
import xcrash.lib.BuildConfig

/**
 * FlowBus 日志策略接口，定义框架内部日志打印的行为规范。
 *
 * 通过实现此接口，用户可以定制 FlowBus 框架的日志输出方式，
 * 例如重定向到第三方日志库、控制日志级别、格式化日志内容等。
 *
 * 自定义实现完成后，调用 [FlowBusLogger.setLoggerStrategy] 进行设置生效。
 *
 * @see FlowBusLogger
 */
interface FlowBusLoggerStrategy {

    /**
     * 打印普通调试日志。
     *
     * 用于记录事件发生的正常流程和数据，方便开发调试和排查问题。
     *
     * @param tag 用于标识日志来源的标签，通常为类名或功能模块名
     * @param eventKey 事件唯一标识，包含事件名称和数据类型信息
     * @param data 事件携带的具体数据，可为空
     *
     * @see FlowBusLogger.log
     */
    fun log(tag: String, eventKey: EventKey<*>, data: Any? = null)

    /**
     * 打印异常错误日志。
     *
     * 用于捕获并输出事件处理过程中的异常和错误信息，辅助定位故障原因。
     *
     * @param tag 用于标识日志来源的标签，通常为类名或功能模块名
     * @param eventKey 事件唯一标识，包含事件名称和数据类型信息
     * @param throwable 捕获的异常对象，包含错误堆栈和消息
     *
     * @see FlowBusLogger.logError
     */
    fun logError(tag: String, eventKey: EventKey<*>, throwable: Throwable)
}

/**
 * FlowBusLogger 是 FlowBus 框架内部的日志管理器，
 * 负责统一控制日志输出行为，支持替换日志策略实现。
 *
 * 框架内所有日志均通过此类进行打印，默认仅在 Debug 模式下输出，
 * 生产环境避免日志冗余影响性能和安全。
 *
 * 通过 [setLoggerStrategy] 方法可设置自定义日志策略，实现日志接入统一管理。
 *
 * 示例用法：
 * ```
 * FlowBusLogger.setLoggerStrategy(object : FlowBusLoggerStrategy {
 *     override fun log(tag: String, eventKey: EventKey<*>, data: Any?) {
 *         AppLogger.i("FlowBus", "[$tag] key=${eventKey.key}, type=${eventKey.type.simpleName}, data=$data")
 *     }
 *
 *     override fun logError(tag: String, eventKey: EventKey<*>, throwable: Throwable) {
 *         AppLogger.e("FlowBus", "[$tag] key=${eventKey.key}, type=${eventKey.type.simpleName}, error=${throwable.message}", throwable)
 *     }
 * })
 * ```
 *
 * @see FlowBusLoggerStrategy
 */
internal object FlowBusLogger {

    @Volatile
    private var strategy: FlowBusLoggerStrategy = DefaultLoggerStrategy()

    /**
     * 设置全局日志策略。
     *
     * 用于替换默认日志实现，定制日志输出方式及格式。
     * 推荐在应用启动时调用，保证日志策略在整个应用生命周期内一致。
     *
     * @param customStrategy 用户自定义的日志策略实现，不能为空
     *
     * @see FlowBusLoggerStrategy
     */
    fun setLoggerStrategy(customStrategy: FlowBusLoggerStrategy) {
        strategy = customStrategy
    }

    /**
     * 打印普通调试日志，委托给当前日志策略实现。
     *
     * @param tag 日志标签，标识日志来源
     * @param eventKey 事件唯一标识
     * @param data 事件携带数据，可为空
     *
     * @see FlowBusLoggerStrategy.log
     */
    fun log(tag: String, eventKey: EventKey<*>, data: Any? = null) {
        strategy.log(tag, eventKey, data)
    }

    /**
     * 打印异常错误日志，委托给当前日志策略实现。
     *
     * @param tag 日志标签，标识日志来源
     * @param eventKey 事件唯一标识
     * @param throwable 捕获的异常对象
     *
     * @see FlowBusLoggerStrategy.logError
     */
    fun logError(tag: String, eventKey: EventKey<*>, throwable: Throwable) {
        strategy.logError(tag, eventKey, throwable)
    }

    /**
     * 内置默认日志策略实现，
     * 仅在 Debug 模式下打印日志，
     * 输出到 Android Logcat，便于快速调试和排查问题。
     */
    private class DefaultLoggerStrategy : FlowBusLoggerStrategy {

        override fun log(tag: String, eventKey: EventKey<*>, data: Any?) {
            if (!BuildConfig.DEBUG) return
            val msg = formatLogMessage(tag, eventKey, data)
            Log.i("FlowBus", msg)
        }

        override fun logError(tag: String, eventKey: EventKey<*>, throwable: Throwable) {
            if (!BuildConfig.DEBUG) return
            val msg = formatErrorMessage(tag, eventKey, throwable)
            Log.e("FlowBus", msg, throwable)
        }

        private fun formatLogMessage(tag: String, eventKey: EventKey<*>, data: Any?): String {
            val safeData = data?.toString() ?: "<null>"
            return "[$tag] key=${eventKey.key}, type=${eventKey.type.simpleName}, data=$safeData"
        }

        private fun formatErrorMessage(tag: String, eventKey: EventKey<*>, throwable: Throwable): String {
            val safeError = throwable.message ?: "<no message>"
            return "[$tag] key=${eventKey.key}, type=${eventKey.type.simpleName}, error=$safeError"
        }
    }
}
