package com.example.sampleview.taskScheduler.core

import com.example.sampleview.taskScheduler.model.TaskStatus
import java.util.concurrent.atomic.AtomicReference

/**
 * 任务状态机，管理任务状态的合法转换。
 *
 * 通过预定义的状态转换规则，保证任务状态变更的有效性和一致性。
 * 支持并发安全的状态读写和原子更新，并可通过回调监听状态变化。
 *
 * @property initialState 任务的初始状态，默认值为 [com.example.sampleview.taskScheduler.model.TaskStatus.PENDING]
 * @property onStateChanged 状态变化回调，接收旧状态和新状态参数，状态变更成功时调用
 */
class TaskStateMachine(
    initialState: TaskStatus = TaskStatus.PENDING,
    private val onStateChanged: ((old: TaskStatus, new: TaskStatus) -> Unit)? = null
) {
    /** 当前状态，使用原子引用保证线程安全 */
    private val _state = AtomicReference(initialState)

    /**
     * 合法的状态转换规则映射表：
     *  - 键为当前状态
     *  - 值为允许转移到的目标状态集合
     */
    private val transitions = mapOf(
        TaskStatus.PENDING to setOf(TaskStatus.PENDING, TaskStatus.RUNNING, TaskStatus.CANCELED),
        TaskStatus.RUNNING to setOf(TaskStatus.RUNNING, TaskStatus.RETRYING, TaskStatus.SUCCESS, TaskStatus.FAILED, TaskStatus.CANCELED),
        TaskStatus.RETRYING to setOf(TaskStatus.RETRYING, TaskStatus.RUNNING, TaskStatus.FAILED, TaskStatus.CANCELED),
        TaskStatus.SUCCESS to emptySet(),
        TaskStatus.FAILED to emptySet(),
        TaskStatus.CANCELED to emptySet()
    )

    /**
     * 获取当前状态。
     *
     * @return 当前的 [TaskStatus]
     */
    val currentState: TaskStatus
        get() = _state.get()

    /**
     * 判断是否可以从当前状态合法转换到指定的新状态。
     *
     * @param newState 目标状态
     * @return 若允许转换返回 true，否则 false
     */
    fun canTransitTo(newState: TaskStatus): Boolean {
        return transitions[currentState]?.contains(newState) == true
    }

    /**
     * 尝试将当前状态转换为新的状态。
     *
     * 如果转换不合法则抛出 [IllegalStateException]。
     * 转换成功后会触发状态变化回调（如果有的话）。
     *
     * @param newState 目标状态
     * @throws IllegalStateException 如果状态转换非法
     * @return 转换成功返回 true，失败（竞争失败）返回 false
     */
    fun transitionTo(newState: TaskStatus): Boolean {
        val oldState = _state.get()
        if (oldState == newState) return true
        if (!canTransitTo(newState)) {
            throw IllegalStateException("Invalid state transition: $oldState → $newState")
        }
        val success = _state.compareAndSet(oldState, newState)
        if (success) {
            onStateChanged?.invoke(oldState, newState)
        }
        return success
    }

}