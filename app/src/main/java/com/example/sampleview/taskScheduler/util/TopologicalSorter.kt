package com.example.sampleview.taskScheduler.util

import com.example.sampleview.taskScheduler.api.Task

/**
 * 拓扑排序工具对象，专门用于对任务列表进行依赖关系排序，
 * 确保所有任务的依赖任务均在它之前执行。
 *
 * 拓扑排序的核心原则是：
 * - 先执行所有依赖项；
 * - 后执行依赖项所在的任务。
 *
 * 如果检测到依赖环（循环依赖），会抛出 [IllegalStateException] 异常，
 * 以防止死循环和错误的任务调度顺序。
 */
object TopologicalSorter {

    /**
     * 拓扑排序入口，保证依赖任务先于被依赖任务。
     *
     * @param tasks 需要排序的任务列表
     * @return 拓扑排序后的任务列表
     * @throws IllegalStateException 如果检测到依赖环抛出异常
     */
    fun sort(tasks: List<Task<*>>): List<Task<*>> {
        // 根据 dependsOn 构建每个任务的依赖任务对象列表
        resolveDependencies(tasks)
        // 保存最终排序结果的集合
        val sorted = mutableListOf<Task<*>>()
        // 已访问完成的任务集合，防止重复访问
        val visited = mutableSetOf<Task<*>>()
        // 当前递归调用栈中正在访问的任务集合，用于环检测
        val visiting = mutableSetOf<Task<*>>()
        // 遍历所有任务，递归访问未访问的任务
        tasks.forEach { task ->
            if (task !in visited) {
                visitTask(task, visited, visiting, sorted)
            }
        }
        return sorted
    }

    /**
     * 根据每个任务声明的依赖（类类型），将实际的依赖任务对象注入进去。
     *
     * - 会根据任务类型创建一个映射表，便于快速查找依赖任务对象。
     * - 若声明的依赖任务类型未在列表中找到，将记录错误日志。
     * - 若找到依赖对象，则通过 [Task.addDependsOn] 注入依赖关系，并记录日志。
     *
     * @param tasks 所有需要构建依赖关系的任务列表
     */
    private fun resolveDependencies(tasks: List<Task<*>>) {
        val taskMap = tasks.associateBy { it::class.java }

        tasks.forEach { task ->
            val deps = task.dependsOn()
                .mapNotNull { depClass ->
                    val depTask = taskMap[depClass]
                    if (depTask == null) {
                        TaskSchedulerLog.e("未找到依赖任务类: ${depClass.simpleName} for ${task.name}")
                    }
                    depTask
                }
            if (deps.isNotEmpty()) {
                val depNames = deps.joinToString(", ") { it.name }
                TaskSchedulerLog.i("任务[${task.name}] 依赖任务: $depNames")
                task.addDependsOn(*deps.toTypedArray())
            }
        }
    }

    /**
     * 递归访问单个任务，进行深度优先遍历并检测环路。
     *
     * @param task 当前访问任务
     * @param visited 已访问完成任务集合
     * @param visiting 当前访问栈中的任务集合，用于环检测
     * @param sorted 排序结果列表，依赖先加入
     */
    private fun visitTask(
        task: Task<*>,
        visited: MutableSet<Task<*>>,
        visiting: MutableSet<Task<*>>,
        sorted: MutableList<Task<*>>,
    ) {
        // 如果当前任务已经在访问栈中，说明出现依赖环，抛出异常
        if (task in visiting) {
            TaskSchedulerLog.e("检测到任务依赖环，任务名=${task.name}")
            throw IllegalStateException("任务依赖有环：${task.name}")
        }
        // 若任务尚未访问，则开始访问
        if (task !in visited) {
            // 标记当前任务进入访问栈
            visiting += task
            // 递归访问所有依赖任务，保证依赖先于当前任务执行
            task.dependencies.forEach { dep ->
                visitTask(dep, visited, visiting, sorted)
            }
            // 当前任务访问完成，移出访问栈
            visiting -= task
            // 标记任务访问完成
            visited += task
            // 将当前任务添加到结果列表，保证依赖先于被依赖任务
            sorted += task
        }
    }

}