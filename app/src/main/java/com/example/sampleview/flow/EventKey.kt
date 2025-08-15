package com.example.sampleview.flow

import kotlin.reflect.KClass

/**
 * EventKey 用于唯一标识一个事件通道，由业务中的字符串 key 和事件类型 [KClass] 组成。
 *
 * 设计目的：
 *  - 通过组合业务标识符和事件类型，实现同一业务 key 下支持多种不同事件类型的区分。
 *  - 提升代码的类型安全性，避免事件混淆。
 *  - 方便作为 Map 的键，管理对应的 Flow、版本号和订阅者计数。
 *
 * @param T 事件数据类型，必须是非空类型
 * @property key 业务事件的唯一标识符，通常是字符串，如 "user_update"、"location_changed" 等
 * @property type 事件的数据类型的 KClass 对象，用于区分同一 key 的不同类型事件
 */
data class EventKey<T : Any>(
    val key: String,
    val type: KClass<T>
)
