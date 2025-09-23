package com.example.sampleview.eventtracker.pipeline

import com.example.sampleview.AppLogger
import com.example.sampleview.eventtracker.dispatcher.EventDispatcher
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.interceptor.RealEventInterceptorChain
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.plugin.EventPlugin

/**
 * 事件处理管道 [EventPipeline]，负责将事件依次经过拦截器链、插件链和事件调度器。
 *
 * ### 处理流程
 * 1. **拦截器链处理**（EventInterceptor）：
 *    - 可对事件进行修改、过滤或阻止。
 *    - 通过 RealEventInterceptorChain 实现链式调用。
 * 2. **插件 before 处理**（EventPlugin.onEventBeforeTrack）：
 *    - 在事件上传前执行扩展逻辑，例如日志记录、埋点统计。
 * 3. **事件调度**（EventDispatcher.dispatch）：
 *    - 根据事件的上传模式选择合适的上传策略（Immediate / Batch）。
 *    - 调用对应策略处理事件。
 * 4. **插件 after 处理**（EventPlugin.onEventAfterTrack）：
 *    - 在事件上传后执行扩展逻辑，例如回调、统计或二次处理。
 *
 * ### 异常处理
 * - 拦截器、插件或 Dispatcher 异常不会抛出到外部。
 * - 所有异常会被捕获并通过 [AppLogger] 记录警告日志，保证事件流程不中断。
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
     * ### 步骤
     * 1. 拷贝拦截器和插件快照，保证流程中列表不被修改。
     * 2. 执行拦截器链，返回最终事件（可被过滤为 null）。
     * 3. 执行插件的 `onEventBeforeTrack` 回调。
     * 4. 调用 [EventDispatcher.dispatch] 将事件分发到对应上传策略。
     * 5. 执行插件的 `onEventAfterTrack` 回调。
     *
     * ### 异常安全
     * - 拦截器、插件或 Dispatcher 内部异常不会中断流程。
     * - 异常会被捕获并打印日志。
     *
     * @param event 待处理的事件
     */
    suspend fun process(event: Event) {
        try {
            val interceptorSnapshot = interceptors.toList()
            val pluginSnapshot = plugins.toList()

            val chain = RealEventInterceptorChain(interceptorSnapshot, 0, event)
            val finalEvent = chain.proceed() ?: return

            pluginSnapshot.forEach { it.onEventBeforeTrack(finalEvent) }

            val result = dispatcher.dispatch(finalEvent)

            pluginSnapshot.forEach { it.onEventAfterTrack(finalEvent, result) }
        } catch (t: Throwable) {
            TrackerLogger.logger.log("EventPipeline.process failed: $t")
        }
    }
}
