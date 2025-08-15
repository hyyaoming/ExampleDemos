package com.example.sampleview.flow

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 表示一个事件槽位（EventSlot），是 FlowBus 中事件发布与订阅的核心桥梁。
 *
 * 每个事件槽封装了：
 * - 一个 [MutableSharedFlow]，用于事件分发；
 * - 当前事件的版本号（[version]），用于粘性事件派发或去重判断；
 *
 * 通常由 [FlowBusCore.getEventSlot] 创建和管理，不建议业务层直接使用。
 *
 * 用法：
 * - 每个 [EventKey] 对应一个独立的 EventSlot；
 * - 粘性事件会通过版本号更新；
 * - 所有事件发送都包装为 [FlowEvent] 后进入该槽。
 *
 * @param flow 当前事件对应的 SharedFlow 流
 * @param version 当前事件的版本号
 *
 * @see FlowEvent
 * @see EventKey
 * @see FlowBusCore
 * @see FlowEventPublisher
 */
data class EventSlot<T : Any>(
    val flow: MutableSharedFlow<FlowEvent<T>>,
    val version: Long
)
