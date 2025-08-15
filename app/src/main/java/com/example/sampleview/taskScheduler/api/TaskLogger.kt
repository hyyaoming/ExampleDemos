package com.example.sampleview.taskScheduler.api

/**
 * 任务日志接口，定义任务调度框架中的日志输出规范。
 *
 * 业务方可以根据需要自定义实现此接口，
 * 以便将日志输出到控制台、文件、远程服务器等不同目标。
 *
 * 提供调试（d）、信息（i）、警告（w）、错误（e）四个级别的方法，
 * 并支持附带异常堆栈的错误日志输出。
 */
interface TaskLogger {
    /** 输出调试级别日志 */
    fun d(tag: String, msg: String)

    /** 输出信息级别日志 */
    fun i(tag: String, msg: String)

    /** 输出警告级别日志 */
    fun w(tag: String, msg: String)

    /** 输出错误级别日志，支持可选异常对象 */
    fun e(tag: String, msg: String, throwable: Throwable? = null)
}