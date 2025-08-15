package com.example.sampleview.taskScheduler.core

import com.example.sampleview.taskScheduler.api.Task
import com.example.sampleview.taskScheduler.api.TaskContext
import com.example.sampleview.taskScheduler.impl.TaskContextImpl
import com.example.sampleview.taskScheduler.model.TaskExecutionResult
import com.example.sampleview.taskScheduler.model.TaskSchedulerResult
import com.example.sampleview.taskScheduler.util.TaskSchedulerLog
import com.example.sampleview.taskScheduler.util.TopologicalSorter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException

/**
 * 任务调度器：支持对一组存在依赖关系的异步任务进行并发调度与执行。
 *
 * 主要功能：
 * - 支持任务间依赖的拓扑排序，确保依赖顺序正确；
 * - 支持最大并发数限制，控制执行资源；
 * - 利用协程实现异步并发执行，提升效率；
 * - 基于线程安全的就绪任务队列调度执行；
 * - 支持任务超时处理、异常捕获与取消响应；
 * - 可随时取消调度器并中止所有未执行任务；
 * - 收集并汇总任务执行的完整结果信息。
 *
 * 工作流程：
 * 1. 使用 [TopologicalSorter] 对任务进行拓扑排序，检测并避免依赖环；
 * 2. 构建 [ExecutionContext]，初始化依赖计数和反向依赖图；
 * 3. 创建固定数量的协程工作线程，从就绪任务队列中获取任务并并发执行；
 * 4. 每个任务完成后通知依赖递减计数，依赖满足的任务将被加入就绪队列；
 * 5. 所有任务完成或调度被取消后，清理资源并返回调度结果。
 *
 * @property tasks           所有待调度执行的任务列表（可存在依赖关系）
 * @property maxConcurrency 最大并发执行任务数，默认使用 CPU 核心数
 * @property taskContext     可选任务上下文（用于在任务间共享数据）
 *
 * @see Task
 * @see TaskExecutionResult
 * @see TaskSchedulerResult
 */
class TaskScheduler private constructor(
    private val tasks: List<Task<*>>,
    private val maxConcurrency: Int,
    private val taskContext: TaskContext,
) {
    /**
     * 当前调度执行上下文，管理任务依赖计数、状态及就绪通道等。
     */
    private var executionContext: ExecutionContext? = null

    /**
     * 定义一个 SupervisorJob，作为协程作用域的父 Job，管理所有子协程的生命周期和取消
     */
    private val job = SupervisorJob()

    /**
     * 创建一个协程作用域，使用默认调度器（Dispatchers.Default）和上面定义的 job 组合，
     * scope 用于启动和管理调度器内部的所有协程，方便统一取消和调度
     */
    private val scope = CoroutineScope(Dispatchers.Default + job)

    /**
     * 任务执行结果收集器，负责保存每个任务的执行结果和状态。
     */
    private val resultCollector = ResultCollector()

    /**
     * 取消当前正在执行的任务调度，
     * 会取消协程作用域，关闭就绪任务通道，安全停止所有调度任务。
     */
    fun cancel() {
        if (!job.isActive) return
        job.cancel()
        executionContext?.clear()
        executionContext = null
        resultCollector.clear()
        TaskSchedulerLog.i("调度器收到取消请求，取消所有任务调度")
    }

    /**
     * 挂起函数，执行所有任务。
     *
     * - 先对任务列表进行拓扑排序，保证依赖先行；
     * - 构建执行上下文并启动调度工作协程池；
     * - 启动执行循环，等待所有任务执行完成或被取消；
     * - 返回包含总耗时及失败任务异常映射的调度结果。
     *
     * @return [com.example.sampleview.taskScheduler.model.TaskSchedulerResult] 包含调度执行总耗时及失败异常详情
     *
     * @throws IllegalStateException 如果任务依赖存在环，会抛出该异常。
     */
    suspend fun executeAll(): TaskSchedulerResult {
        val startTime = System.currentTimeMillis()
        val sorted = TopologicalSorter.sort(tasks)
        val context = ExecutionContext.from(sorted)
        this@TaskScheduler.executionContext = context
        runExecutionLoop(context)
        return handleTaskResult(startTime)
    }

    /**
     * 启动固定数量的协程工作线程，从就绪任务通道消费任务并执行，
     * 直到所有任务完成且通道关闭。
     *
     * @param context   执行上下文，包含任务依赖和就绪通道
     */
    private suspend fun runExecutionLoop(context: ExecutionContext) {
        val workers = List(maxConcurrency) {
            scope.launch {
                workerLoop(context)
            }
        }
        workers.joinAll()
    }

    /**
     * 工作协程循环体，从就绪任务通道中取任务执行。
     *
     * - 检测协程是否已取消，及时停止；
     * - 执行任务，捕获异常；
     * - 执行完成后通知依赖任务依赖计数递减；
     * - 记录任务执行结果；
     * - 根据执行上下文状态，尝试关闭通道。
     *
     * @param scope     当前协程作用域，支持取消检测
     * @param context   执行上下文，管理任务依赖和结果
     */
    private suspend fun workerLoop(context: ExecutionContext) {
        while (scope.isActive) {
            val task = context.pollReadyTask() ?: break
            context.taskStarted()
            try {
                val result = task.execute(taskContext)
                resultCollector.recordResult(task, TaskExecutionResult.Success(result))
                context.onTaskCompleted(task)
            } catch (e: CancellationException) {
                resultCollector.recordResult(task, TaskExecutionResult.Cancelled)
            } catch (e: Throwable) {
                resultCollector.recordResult(task, TaskExecutionResult.Failure(e))
            } finally {
                context.taskFinished()
            }
            yield()
        }
    }

    /**
     * 汇总所有任务执行结果，输出日志并返回调度最终结果。
     *
     * @param startTime 调度开始时间戳，毫秒
     * @return [TaskSchedulerResult] 包含总耗时及失败任务异常映射
     */
    private fun handleTaskResult(startTime: Long): TaskSchedulerResult {
        val totalTime = System.currentTimeMillis() - startTime
        resultCollector.logSummary(tasks, totalTime)
        return TaskSchedulerResult(totalTime, resultCollector.collectFailures())
    }

    companion object {
        /**
         * 创建一个 TaskScheduler 实例。
         *
         * @param tasks 要调度执行的任务列表
         * @param maxConcurrency 最大并发数，默认 CPU 核心数
         * @param taskContext 任务上下文（可选），默认使用 TaskContextImpl
         */
        fun create(
            tasks: List<Task<*>>,
            maxConcurrency: Int = Runtime.getRuntime().availableProcessors(),
            taskContext: TaskContext = TaskContextImpl()
        ): TaskScheduler {
            return TaskScheduler(tasks, maxConcurrency, taskContext)
        }
    }
}