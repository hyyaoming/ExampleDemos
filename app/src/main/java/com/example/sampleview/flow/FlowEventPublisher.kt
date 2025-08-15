package com.example.sampleview.flow

/**
 * FlowEventPublisher 是 FlowBus 的核心事件发送器，用于将事件推送到 SharedFlow 管道中。
 *
 * 主要职责：
 * - 支持普通事件发送（非粘性）：仅当前订阅者能接收到；
 * - 支持粘性事件发送：即使订阅者稍后注册，也能收到最后一条事件；
 * - 同时提供非挂起（tryEmit）和挂起（emit）两种发送方式；
 *
 * 不建议业务层直接调用本类，应通过上层封装的 FlowBus 接口使用。
 *
 * @see FlowBusCore
 * @see FlowEvent
 * @see EventKey
 * @see FlowBusLogger
 */
@PublishedApi
internal object FlowEventPublisher {

    /**
     * 同步发送普通事件（非粘性）。
     *
     * 使用 tryEmit 将事件发送到对应 SharedFlow，失败时不会阻塞，
     * 适用于不要求可靠送达的 UI 通知、简单广播等场景。
     *
     * @param eventKey 事件标识（由 key + 类型构成）
     * @param value 要发送的事件内容
     *
     * @see postStickyInternal
     * @see postInternalSuspend
     * @see FlowBusCore.getEventSlot
     */
    fun <T : Any> postInternal(eventKey: EventKey<T>, value: T) =
        postEvent(eventKey, value, isSticky = false)

    /**
     * 同步发送粘性事件（可缓存）。
     *
     * 粘性事件会被缓存，后续订阅者在注册时会立即收到最新一条事件。
     * 适用于需要保留“当前状态”的事件，如登录状态、配置更新等。
     *
     * @param eventKey 事件标识
     * @param value 要发送的事件内容
     *
     * @see postInternal
     * @see postStickyInternalSuspend
     * @see FlowBusCore.getEventSlot
     */
    fun <T : Any> postStickyInternal(eventKey: EventKey<T>, value: T) =
        postEvent(eventKey, value, isSticky = true)

    /**
     * 挂起发送普通事件（非粘性）。
     *
     * 使用 emit 方式发送事件，当 SharedFlow 缓冲区满时将挂起等待。
     * 适用于协程中保证发送成功的场景，如重要业务事件传递。
     *
     * @param eventKey 事件标识
     * @param value 要发送的事件内容
     *
     * @see postStickyInternalSuspend
     * @see postInternal
     * @see FlowBusCore.getEventSlot
     */
    suspend fun <T : Any> postInternalSuspend(eventKey: EventKey<T>, value: T) =
        postEventSuspend(eventKey, value, isSticky = false)

    /**
     * 挂起发送粘性事件（可缓存）。
     *
     * 粘性事件通过 suspend emit 发送，保证发送成功，允许后续订阅者接收。
     * 适用于重要状态广播或异步数据更新事件。
     *
     * @param eventKey 事件标识
     * @param value 要发送的事件内容
     *
     * @see postInternalSuspend
     * @see postStickyInternal
     * @see FlowBusCore.getEventSlot
     */
    suspend fun <T : Any> postStickyInternalSuspend(eventKey: EventKey<T>, value: T) =
        postEventSuspend(eventKey, value, isSticky = true)

    /**
     * 统一的事件发送实现（同步）。
     *
     * 使用 tryEmit 将事件包裹成 FlowEvent 并发送至 SharedFlow。
     * 如果失败（通常是没有订阅者或 buffer 满），则记录错误日志。
     *
     * @param eventKey 事件唯一标识（用于找到对应的 FlowEventSlot）
     * @param value 事件数据内容
     * @param isSticky 是否为粘性事件
     *
     * @see FlowEvent
     * @see FlowBusCore.getEventSlot
     * @see FlowBusLogger.log
     * @see FlowBusLogger.logError
     */
    private fun <T : Any> postEvent(eventKey: EventKey<T>, value: T, isSticky: Boolean) {
        val eventSlot = FlowBusCore.getEventSlot(eventKey, value, isSticky)
        val emitted = eventSlot.flow.tryEmit(FlowEvent(eventSlot.version, value))
        if (emitted) {
            FlowBusLogger.log(
                if (isSticky) "Post StickyEvent" else "Post Event",
                eventKey, value
            )
        } else {
            FlowBusLogger.logError(
                "Post Failed",
                eventKey,
                IllegalStateException("tryEmit failed: no active collectors or buffer full")
            )
        }
    }

    /**
     * 统一的事件发送实现（挂起）。
     *
     * 使用 suspend emit 将事件发送到 SharedFlow，可挂起等待消费者处理。
     * 对于粘性事件，会保存到缓存中供后续订阅者立即接收。
     *
     * @param eventKey 事件唯一标识（用于匹配事件流）
     * @param value 要发送的数据
     * @param isSticky 是否为粘性事件（决定是否缓存）
     *
     * @see FlowEvent
     * @see FlowBusCore.getEventSlot
     * @see FlowBusLogger.log
     */
    private suspend fun <T : Any> postEventSuspend(eventKey: EventKey<T>, value: T, isSticky: Boolean) {
        val eventSlot = FlowBusCore.getEventSlot(eventKey, value, isSticky)
        eventSlot.flow.emit(FlowEvent(eventSlot.version, value))
        FlowBusLogger.log(
            if (isSticky) "Post StickyEvent (suspend)" else "Post Event (suspend)",
            eventKey, value
        )
    }
}
