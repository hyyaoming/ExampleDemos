package com.example.sampleview.eventtracker.pipeline

import com.example.sampleview.AppLogger
import com.example.sampleview.eventtracker.dispatcher.EventDispatcher
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.interceptor.RealEventInterceptorChain
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.plugin.EventPlugin


/**
 * 事件处理管道，用于将事件依次经过拦截器、插件和调度器。
 *
 * 流程：
 * 1. 拦截器链处理：可修改、过滤或阻止事件
 * 2. 插件 before 处理：在事件上传前执行扩展逻辑（如日志、埋点）
 * 3. Dispatcher 调度上传策略：根据事件上传模式选择对应策略处理事件
 * 4. 插件 after 处理：在事件上传后执行扩展逻辑（如统计、回调）
 *
 * @property interceptors 已注册的事件拦截器列表
 * @property plugins 已注册的事件插件列表
 * @property dispatcher 事件调度器，用于将事件分发到具体上传策略
 */
class EventPipeline(
    private val interceptors: List<EventInterceptor>,
    private val plugins: List<EventPlugin>,
    private val dispatcher: EventDispatcher,
) {

    /**
     * 处理单个事件的完整流程。
     *
     * - 先经过拦截器链（可修改或阻止事件）
     * - 然后执行插件的 `onEventBeforeTrack` 回调
     * - 再通过 [dispatcher] 调度上传
     * - 最后执行插件的 `onEventAfterTrack` 回调
     *
     * 异常安全：
     * - 拦截器、插件或 Dispatcher 异常不会抛出，会被捕获并记录日志
     *
     * @param event 待处理事件
     */
    suspend fun process(event: Event) {
        try {
            // 拷贝当前拦截器和插件快照，避免中途修改
            val interceptorSnapshot = interceptors.toList()
            val pluginSnapshot = plugins.toList()

            // 1. 执行拦截器链
            val chain = RealEventInterceptorChain(interceptorSnapshot, 0, event)
            val finalEvent = chain.proceed() ?: return

            // 2. 执行插件 before 回调
            pluginSnapshot.forEach { it.onEventBeforeTrack(finalEvent) }

            // 3. 调用 Dispatcher 处理事件
            val result = try {
                dispatcher.dispatch(finalEvent)
            } catch (t: Throwable) {
                EventUploadResult.Failure(emptyList(), t)
            }

            // 4. 执行插件 after 回调
            pluginSnapshot.forEach { it.onEventAfterTrack(finalEvent, result) }
        } catch (t: Throwable) {
            AppLogger.w("EventPipeline", "EventPipeline.process failed: $t")
        }
    }
}
