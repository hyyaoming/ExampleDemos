package com.example.sampleview

import android.util.Log

object AppLogger {

    private const val DEFAULT_TAG = "AppLogger"
    private const val STACK_DEPTH_FOR_CALLER = 4          // 调整这个数字可以精细控制“谁算调用方”
    private const val MAX_STACK_LINES = 20            // 完整栈最多打印多少行

    /** 返回形如  [WebSocketActivity.kt:42#doSomething] 的调用点信息 */
    private fun callerInfo(depth: Int = STACK_DEPTH_FOR_CALLER): String {
        val stack = Throwable().stackTrace
        if (stack.size <= depth) return "[unknown]"
        val e = stack[depth]
        return "[${e.fileName}:${e.lineNumber}#${e.methodName}]"
    }

    /** 如需排查复杂问题，可以打开 `printFullStack=true` 来顺带输出完整堆栈 */
    private fun formatMsg(
        msg: String,
        depth: Int = STACK_DEPTH_FOR_CALLER,
        printFullStack: Boolean = false
    ): String {
        // ① 调用点
        val builder = StringBuilder(callerInfo(depth))
            .append(' ')
            .append(msg)

        // ② 可选：追加完整栈
        if (printFullStack) {
            val stack = Throwable().stackTrace
            val stackLines = stack
                .drop(depth)                      // 跳过 Logger 自身 & 之前的若干层
                .take(MAX_STACK_LINES)
                .joinToString(separator = "\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            builder.append("\n└─Stack:\n").append(stackLines)
        }
        return builder.toString()
    }

    @JvmStatic
    fun d(
        tag: String = DEFAULT_TAG,
        msg: String,
        printFullStack: Boolean = false
    ) {
        Log.d(tag, formatMsg(msg, printFullStack = printFullStack))
    }

    @JvmStatic
    fun i(
        tag: String = DEFAULT_TAG,
        msg: String,
        printFullStack: Boolean = false
    ) {
        Log.i(tag, formatMsg(msg, printFullStack = printFullStack))
    }

    @JvmStatic
    fun w(
        tag: String = DEFAULT_TAG,
        msg: String,
        printFullStack: Boolean = false
    ) {
        Log.w(tag, formatMsg(msg, printFullStack = printFullStack))
    }

    @JvmStatic
    fun e(
        tag: String = DEFAULT_TAG,
        msg: String,
        throwable: Throwable? = null,
        printFullStack: Boolean = false
    ) {
        Log.e(tag, formatMsg(msg, printFullStack = printFullStack), throwable)
    }
}