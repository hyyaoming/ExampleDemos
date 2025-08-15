package com.example.sampleview.flow

/**
 * 封装通过 FlowBus 传递的事件数据，支持事件版本控制机制。
 *
 * 事件版本号 [version] 用于标记事件的唯一性，
 * 每次发送事件时递增，订阅端可以通过比较版本号避免重复消费旧事件，
 * 这在结合生命周期感知收集（如 [androidx.lifecycle.repeatOnLifecycle]）时尤其重要，
 * 可防止因界面重建导致的事件重复处理。
 *
 * @param T 事件携带的数据类型
 * @property version 事件的版本号，默认值为 0，表示初始无事件或未设置版本
 * @property data 实际传递的事件数据，可为 null，表示无具体数据
 *
 * 使用示例：
 * ```kotlin
 * val event = FlowEvent(version = 1L, data = "Hello World")
 * ```
 *
 * 订阅时可通过版本判断过滤重复事件：
 * ```kotlin
 * if (event.version > lastHandledVersion) {
 *     // 处理事件
 *     lastHandledVersion = event.version
 * }
 * ```
 *
 * @see FlowBusCore
 * @see androidx.lifecycle.repeatOnLifecycle
 */
data class FlowEvent<T>(
    val version: Long = 0L,
    val data: T? = null
)
