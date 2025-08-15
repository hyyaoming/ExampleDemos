package com.example.sampleview.taskScheduler.model

/**
 * 封装任务执行结果，支持成功、失败（异常）和取消状态。
 * 方便集中管理任务执行状态和异常处理。
 */
sealed class TaskExecutionResult {
    /**
     * 任务执行成功，携带执行结果。
     */
    data class Success<T>(val result: T) : TaskExecutionResult()

    /**
     * 任务执行失败，携带异常信息。
     */
    data class Failure(val throwable: Throwable) : TaskExecutionResult()

    /**
     * 任务被取消。
     */
    data object Cancelled : TaskExecutionResult()
}