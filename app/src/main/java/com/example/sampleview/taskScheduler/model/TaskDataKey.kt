package com.example.sampleview.taskScheduler.model

import kotlin.reflect.KClass

/**
 * 类型安全的上下文键，用于标识 TaskContext 中存储的数据项。
 * 通过泛型参数 T 指定该键对应的值的类型，避免类型转换错误。
 *
 * @param T 存储值的类型
 * @property key 键的名称，便于调试和日志
 * @property type 类型
 */
class TaskDataKey<T : Any>(val key: String, val type: KClass<T>)