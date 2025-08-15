package com.example.sampleview.taskScheduler.core

import com.example.sampleview.taskScheduler.api.Task
import com.example.sampleview.taskScheduler.util.TaskSchedulerLog
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 执行上下文：封装任务调度所需的依赖关系、就绪任务队列及执行状态管理。
 *
 * 该类负责管理：
 * - 每个任务的剩余未完成依赖数（remainingDepends）；
 * - 任务的反向依赖图（taskGraph），即记录某任务完成后应唤醒的后续任务；
 * - 就绪任务队列（readyQueue），用于存放所有依赖满足、可以立即执行的任务；
 * - 当前活跃执行的任务数（activeJobCount），用于判断调度是否全部完成。
 *
 * 核心职责：
 * - 初始化任务调度上下文（通过 from() 工厂方法）；
 * - 在任务完成时递减依赖任务的依赖计数，并将就绪任务放入执行队列；
 * - 提供线程安全的任务获取与投递方法；
 * - 判断是否可以结束调度并清理状态。
 *
 * @property readyQueue 线程安全的就绪任务队列，存放所有依赖已满足的任务。
 * @property remainingDepends 每个任务剩余的未完成依赖数量，用于判断是否可执行。
 * @property taskGraph 任务反向依赖图，记录哪些任务依赖当前任务。
 */
class ExecutionContext private constructor(
    private val readyQueue: ConcurrentLinkedQueue<Task<*>>,
    internal val remainingDepends: MutableMap<Task<*>, Int>,
    internal val taskGraph: Map<Task<*>, List<Task<*>>>
) {

    /**
     * 当前活跃的任务协程数量，用于判断是否所有任务执行完毕
     */
    private val activeJobCount = AtomicInteger(0)

    /**
     * 标记一个任务开始执行，增加活跃任务计数
     */
    fun taskStarted() {
        activeJobCount.incrementAndGet()
    }

    /**
     * 标记一个任务结束执行，减小活动任务计数
     */
    fun taskFinished() {
        activeJobCount.decrementAndGet()
    }

    /**
     * 取出一个就绪任务，如果队列为空则返回 null
     */
    fun pollReadyTask(): Task<*>? {
        return readyQueue.poll()
    }

    /**
     * 将一个任务加入就绪队列，供调度器执行
     */
    fun enqueueReadyTask(task: Task<*>) {
        readyQueue.offer(task)
    }

    /**
     * 某任务完成后调用，更新所有依赖该任务的任务的依赖计数。
     * 当某个依赖任务的计数减为0时，表示该任务的所有依赖已满足，可以加入就绪队列等待执行。
     *
     * @param task 已完成的任务
     */
    fun onTaskCompleted(task: Task<*>) {
        val dependents = taskGraph[task].orEmpty()
        for (dependent in dependents) {
            val newCount = (remainingDepends[dependent] ?: continue) - 1
            if (newCount < 0) {
                TaskSchedulerLog.w("任务[${dependent.name}]依赖计数异常 < 0")
                continue
            }
            remainingDepends[dependent] = newCount
            if (newCount == 0) {
                enqueueReadyTask(dependent)
            }
        }
    }

    /**
     * 清理所有状态
     */
    fun clear() {
        activeJobCount.set(0)
        readyQueue.clear()
        remainingDepends.clear()
    }

    companion object {
        /**
         * 根据传入的任务列表构建一个 ExecutionContext 实例，
         * 包含任务依赖关系图、每个任务剩余的未完成依赖计数、
         * 以及一个包含所有“就绪任务”的线程安全队列。
         *
         * 初始化流程：
         * 1. 统计每个任务的未完成依赖数（remainingDepends）；
         * 2. 构建任务的反向依赖图（taskGraph），即记录“谁依赖我”；
         * 3. 将所有不依赖其他任务的“根任务”加入就绪队列（readyQueue）。
         *
         * @param tasks 需要调度的任务列表
         * @return 构建好的 ExecutionContext 实例
         */
        fun from(tasks: List<Task<*>>): ExecutionContext {
            // 每个任务对应的 "尚未完成的依赖数"
            val remainingDepends = mutableMapOf<Task<*>, Int>()
            // 任务反向依赖图，记录"当前任务被哪些任务依赖"
            val taskGraph = mutableMapOf<Task<*>, MutableList<Task<*>>>()
            // 遍历所有任务，构建依赖计数和反向依赖图
            for (task in tasks) {
                val depends = task.dependencies
                // 初始化该任务的依赖数量
                remainingDepends[task] = depends.size
                // 记录：该任务被哪些任务依赖
                depends.forEach { dep ->
                    taskGraph.getOrPut(dep) { mutableListOf() }.add(task)
                }
            }
            // 创建线程安全的就绪任务队列，用于存放“所有依赖已完成”的任务
            val readyQueue = ConcurrentLinkedQueue<Task<*>>()
            // 所有不依赖任何任务的 "根任务"，直接加入 readyQueue 等待执行
            remainingDepends.filter { it.value == 0 }.keys.forEach { task ->
                readyQueue.offer(task)
            }
            return ExecutionContext(readyQueue, remainingDepends, taskGraph)
        }
    }
}
