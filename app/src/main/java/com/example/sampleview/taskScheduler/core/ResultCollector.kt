package com.example.sampleview.taskScheduler.core

import com.example.sampleview.taskScheduler.api.Task
import com.example.sampleview.taskScheduler.util.TaskSchedulerLog
import com.example.sampleview.taskScheduler.model.TaskExecutionResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 结果收集器，用于管理和统计任务调度过程中各任务的执行结果。
 *
 * 主要职责包括：
 * - 记录每个任务的执行结果（成功、失败、取消）；
 * - 统计各状态任务数量，便于调度完成时快速汇总；
 * - 提供查询单个任务结果和所有任务结果的接口；
 * - 提供收集所有失败任务异常的功能；
 * - 支持清理所有结果数据，方便重复使用；
 * - 支持打印调度结果摘要日志，便于调试和监控。
 */
class ResultCollector {
    /**
     * 线程安全的任务结果映射，key 为任务实例，value 为任务执行结果。
     */
    private val results = ConcurrentHashMap<Task<*>, TaskExecutionResult>()

    /**
     * 成功任务计数，线程安全。
     */
    private val successCount = AtomicInteger(0)

    /**
     * 失败任务计数，线程安全。
     */
    private val failureCount = AtomicInteger(0)

    /**
     * 取消任务计数，线程安全。
     */
    private val cancelledCount = AtomicInteger(0)

    /**
     * 记录一个任务的执行结果，并更新对应的计数统计。
     *
     * @param task 任务实例
     * @param result 任务执行结果（成功、失败、取消）
     */
    fun recordResult(task: Task<*>, result: TaskExecutionResult) {
        results[task] = result
        when (result) {
            is TaskExecutionResult.Success<*> -> successCount.incrementAndGet()
            is TaskExecutionResult.Failure -> failureCount.incrementAndGet()
            is TaskExecutionResult.Cancelled -> cancelledCount.incrementAndGet()
        }
    }

    /**
     * 查询指定任务的执行结果。
     *
     * @param task 任务实例
     * @return 该任务的执行结果，可能为 null（表示未记录）
     */
    fun getResult(task: Task<*>): TaskExecutionResult? = results[task]

    /**
     * 获取所有任务及其执行结果的映射。
     *
     * @return 任务到结果的映射集合
     */
    fun getAllResults(): Map<Task<*>, TaskExecutionResult> = results

    /**
     * 收集所有执行失败的任务及对应的异常信息。
     *
     * @return 失败任务与异常的映射
     */
    fun collectFailures(): Map<Task<*>, Throwable> {
        val failures = results.filterValues { it is TaskExecutionResult.Failure }
        val failureMap = failures.mapValues { entry ->
            (entry.value as TaskExecutionResult.Failure).throwable
        }
        return failureMap
    }

    /**
     * 清理所有已记录的任务执行结果和计数统计，
     * 适用于任务调度重用时重置状态。
     */
    fun clear() {
        results.clear()
        successCount.set(0)
        failureCount.set(0)
        cancelledCount.set(0)
    }

    /**
     * 输出任务调度的结果摘要日志，
     * 包括每个任务的状态及耗时，以及整体成功、失败、取消数量统计。
     *
     * @param tasks 调度任务列表，用于输出名称和顺序
     * @param totalTime 调度总耗时，单位毫秒
     */
    fun logSummary(tasks: List<Task<*>>, totalTime: Long) {
        tasks.forEach {
            val status = when (val r = results[it]) {
                is TaskExecutionResult.Success<*> -> "成功"
                is TaskExecutionResult.Failure -> "失败: ${r.throwable.message}"
                is TaskExecutionResult.Cancelled -> "已取消"
                else -> "未知"
            }
            TaskSchedulerLog.i("任务[${it.name}] 状态=$status 耗时=${it.executionTimeMillis}ms")
        }
        val summary = "成功: ${successCount.get()}, 失败: ${failureCount.get()}, 取消: ${cancelledCount.get()}"
        TaskSchedulerLog.i("任务调度完成，总耗时 $totalTime ms，$summary")
    }
}
