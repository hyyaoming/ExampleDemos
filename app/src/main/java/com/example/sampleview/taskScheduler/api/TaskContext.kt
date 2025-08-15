package com.example.sampleview.taskScheduler.api

import com.example.sampleview.taskScheduler.model.TaskDataKey

/**
 * 任务执行上下文接口，提供线程安全的键值存储和访问能力。
 * 使用类型安全的 [TaskDataKey] 作为键，避免了传统字符串键的不安全和易错。
 */
interface TaskContext {

    /**
     * 根据类型安全键获取对应的值，若不存在返回 null。
     *
     * @param T 值的类型
     * @param key 类型安全键
     * @return 存储的值或 null
     */
    fun <T : Any> get(key: TaskDataKey<T>): T?

    /**
     * 存储一个值到上下文中，关联指定的类型安全键。
     *
     * @param T 值的类型
     * @param key 类型安全键
     * @param value 要存储的值
     */
    fun <T : Any> put(key: TaskDataKey<T>, value: T)

    /**
     * 判断上下文是否包含指定的键。
     *
     * @param key 类型安全键
     * @return true 表示包含该键
     */
    fun containsKey(key: TaskDataKey<*>): Boolean

    /**
     * 从上下文中移除指定键对应的值。
     *
     * @param key 类型安全键
     */
    fun remove(key: TaskDataKey<*>)
}