package com.example.sampleview.taskScheduler.api

import com.example.sampleview.taskScheduler.model.TaskStatus
import kotlinx.coroutines.CoroutineDispatcher

interface Task<T> : Comparable<Task<*>> {
    /** 任务唯一ID */
    val id: String

    /** 任务名称 */
    val name: String

    /** 任务优先级，数值越大优先执行 */
    val priority: Int

    /** 重试策略，null 表示不重试 */
    val retryStrategy: RetryStrategy?

    /** 协程调度器 */
    val dispatcher: CoroutineDispatcher

    /** 超时时间，0 表示不超时 */
    val timeoutMillis: Long

    /** 延迟启动时间，毫秒 */
    val delayMillis: Long

    /** 当前任务依赖的任务列表 */
    val dependencies: List<Task<*>>

    /** 任务执行结果，初始为 null */
    val result: T?

    /** 当前状态 */
    val status: TaskStatus

    /** 任务耗时，毫秒 */
    val executionTimeMillis: Long

    /** 添加依赖任务，支持链式调用 */
    fun addDependsOn(vararg tasks: Task<*>): Task<T>

    /** Task 可通过 Class 指定依赖任务 */
    fun dependsOn(): List<Class<out Task<*>>> = emptyList()

    /** 取消任务 */
    fun cancel()

    /** 调用该方法的协程会被挂起，直到任务完成信号被触发 */
    suspend fun awaitCompletion()

    /** 执行任务，挂起函数,支持获取依赖任务的处理结果 */
    suspend fun execute(taskContext: TaskContext): T?

    /** 任务开始时回调，默认空实现 */
    fun onStart() {}

    /** 任务成功时回调，默认空实现 */
    fun onSuccess(result: T?) {}

    /** 任务失败时回调，默认空实现 */
    fun onFailure(e: Throwable) {}

    /** 任务重试时回调，默认空实现 */
    fun onRetry(attempt: Int, e: Throwable) {}

    /** 任务取消时回调，默认空实现 */
    fun onCanceled() {}

    /**
     * 按优先级降序排序，优先级高的任务优先执行。
     *
     * @param other 另一任务用于比较
     * @return 比较结果，优先级高的排前面
     */
    override fun compareTo(other: Task<*>): Int = this.priority.compareTo(other.priority) * -1
}