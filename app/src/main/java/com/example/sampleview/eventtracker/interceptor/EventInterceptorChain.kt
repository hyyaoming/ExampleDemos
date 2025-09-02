package com.example.sampleview.eventtracker.interceptor

import com.example.sampleview.eventtracker.model.Event

/**
 * 事件拦截器链，用于管理多个 [EventInterceptor] 的顺序执行。
 *
 * 每个拦截器在调用 [EventInterceptor.intercept] 时都会接收到一个 [EventInterceptorChain] 实例，
 * 通过 [proceed] 可以继续调用链上的下一个拦截器，最终得到最终的 [Event] 或 null。
 *
 * 你可以通过 [modifyEvent] 在当前事件的基础上修改属性，然后继续执行链。
 */
interface EventInterceptorChain {

    /**
     * 当前链中正在处理的事件。
     */
    val event: Event

    /**
     * 返回一个新的链实例，将事件替换为 [event]。
     *
     * 用于在链中创建修改后的事件副本，继续向下执行。
     *
     * @param event 新的事件对象
     * @return 返回一个新的 [EventInterceptorChain] 实例
     */
    fun withEvent(event: Event): EventInterceptorChain

    /**
     * 继续执行链中的下一个拦截器。
     *
     * @return 最终拦截后的 [Event] 对象，如果返回 null 表示事件被拦截，不再上报
     */
    suspend fun proceed(): Event?

    /**
     * 在当前事件副本上执行 [block] 对事件进行修改，然后继续执行链。
     *
     * - 会复制事件的 [Event.properties]，避免直接修改原事件
     * - 修改后的事件会通过 [withEvent] 包装并调用 [proceed] 继续链式执行
     *
     * 示例：
     * ```
     * chain.modifyEvent {
     *     properties["activityPath"] = ActivityPathTracker.getPath()
     * }
     * ```
     *
     * @param block 在事件副本上进行修改
     * @return 链中最终返回的事件对象，或 null 表示被拦截
     */
    suspend fun modifyEvent(block: Event.() -> Unit): Event?
}
