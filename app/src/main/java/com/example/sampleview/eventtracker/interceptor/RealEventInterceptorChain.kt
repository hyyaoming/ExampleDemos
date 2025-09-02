package com.example.sampleview.eventtracker.interceptor

import com.example.sampleview.eventtracker.model.Event

/**
 * [EventInterceptorChain] 的具体实现，用于顺序执行一组 [EventInterceptor]。
 *
 * @property interceptors 拦截器列表
 * @property index 当前正在执行的拦截器索引
 * @property event 当前事件对象
 */
data class RealEventInterceptorChain(
    val interceptors: List<EventInterceptor>,
    val index: Int,
    override val event: Event,
) : EventInterceptorChain {

    /**
     * 创建一个新的链实例，将事件替换为 [event]。
     *
     * @param event 新的事件对象
     * @return 返回一个新的 [RealEventInterceptorChain]，索引保持不变
     */
    override fun withEvent(event: Event): EventInterceptorChain = copy(event = event)

    /**
     * 执行链中的下一个拦截器。
     *
     * - 如果已经没有更多拦截器，返回当前事件
     * - 否则调用当前索引的拦截器的 [EventInterceptor.intercept] 方法，并传入新的链（索引 +1）
     *
     * @return 拦截后得到的事件，或者 null 表示事件被拦截
     */
    override suspend fun proceed(): Event? {
        return if (index >= interceptors.size) {
            event
        } else {
            interceptors[index].intercept(copy(index = index + 1))
        }
    }

    /**
     * 在当前事件副本上执行 [block] 对事件进行修改，然后继续执行链。
     *
     * - 会复制事件的 [Event.properties]，避免直接修改原事件
     * - 修改后的事件会通过 [withEvent] 包装，并调用 [proceed] 继续链式执行
     *
     * 示例：
     * ```
     * chain.modifyEvent {
     *     properties["userId"] = getCurrentUserId()
     * }
     * ```
     *
     * @param block 在事件副本上进行修改
     * @return 链中最终返回的事件对象，或 null 表示被拦截
     */
    override suspend fun modifyEvent(block: Event.() -> Unit): Event? {
        val copy = event.copy(properties = event.properties.toMutableMap())
        copy.block()
        return withEvent(copy).proceed()
    }
}
