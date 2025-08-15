package com.example.sampleview.taskScheduler.impl

import com.example.sampleview.taskScheduler.model.TaskDataKey
import com.example.sampleview.taskScheduler.api.TaskContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务上下文接口的线程安全实现，基于 ConcurrentHashMap。
 *
 * 用于在任务执行过程中传递和共享数据，
 * 类似 Android 中的 Bundle 或 View 的 Tag。
 */
class TaskContextImpl : TaskContext {
    /**
     * 线程安全的键值存储，支持并发读写
     */
    private val dataMap = ConcurrentHashMap<TaskDataKey<*>, Any>()

    /**
     * 根据键获取对应的值，泛型支持任意类型转换。
     *
     * @param key 数据键
     * @return 如果存在则返回对应类型的值，否则返回 null
     */
    override fun <T : Any> get(key: TaskDataKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return dataMap[key] as? T
    }

    /**
     * 存储键值对数据，支持任意类型。
     *
     * @param key 数据键
     * @param value 需要存储的值
     */
    override fun <T : Any> put(key: TaskDataKey<T>, value: T) {
        dataMap[key] = value as Any
    }

    /**
     * 判断是否包含指定键。
     *
     * @param key 数据键
     * @return 如果包含则返回 true，否则 false
     */
    override fun containsKey(key: TaskDataKey<*>): Boolean {
        return dataMap.containsKey(key)
    }

    /**
     * 删除指定键对应的数据。
     *
     * @param key 数据键
     */
    override fun remove(key: TaskDataKey<*>) {
        dataMap.remove(key)
    }


    /**
     * 简化版，利用内联函数和 reified，让调用更简洁类型安全。
     */
    inline fun <reified T : Any> get(key: String): T? {
        return get(TaskDataKey(key, T::class))
    }

    inline fun <reified T : Any> put(key: String, value: T) {
        put(TaskDataKey(key, T::class), value)
    }
}