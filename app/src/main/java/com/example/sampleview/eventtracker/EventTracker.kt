package com.example.sampleview.eventtracker

import android.app.Application
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.interceptor.RealEventInterceptorChain
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.plugin.EventPlugin
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue
import com.example.sampleview.eventtracker.strategy.DefaultUploadStrategy
import com.example.sampleview.eventtracker.strategy.EventUploadStrategy
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.HttpUploader
import com.example.sampleview.eventtracker.upload.RetryingUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 事件跟踪中心，用于统一管理事件的拦截、处理和上报。
 *
 * ## 功能
 * 1. 支持事件拦截器 [EventInterceptor]，可在事件上报前修改事件内容。
 * 2. 支持插件 [EventPlugin]，可在事件上报前后执行额外逻辑。
 * 3. 支持事件队列 [EventQueue]，可缓存事件，实现批量上报。
 * 4. 支持上报策略 [EventUploadStrategy]，根据事件配置决定立即上报或批量入队。
 * 5. 提供异步安全的 track、flush 方法，基于协程执行。
 *
 * ## 使用示例
 * ```
 * EventTracker.init(application)
 * EventTracker.track(Event.Builder("page_view").build())
 * EventTracker.flushEvent()
 * ```
 */
object EventTracker {

    /** 已注册的事件拦截器列表 */
    private val interceptors = mutableListOf<EventInterceptor>()

    /** 已注册的事件插件列表 */
    private val plugins = mutableListOf<EventPlugin>()

    /** Application 上下文 */
    private lateinit var context: Application

    /** 事件队列，用于缓存和管理事件 */
    private lateinit var queue: EventQueue

    /** 事件上报器，用于将事件上传到服务端 */
    private lateinit var uploader: EventUploader

    /** 事件上传策略 */
    private var uploadStrategy: EventUploadStrategy = DefaultUploadStrategy()

    /** 协程作用域，用于异步执行事件处理逻辑 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 初始化事件跟踪器。
     *
     * @param context Application 上下文
     * @param uploader 可选自定义上传器，默认为 [RetryingUploader] 包裹 [HttpUploader]
     * @param queue 可选自定义事件队列，默认为 [InMemoryEventQueue]
     * @param strategy 可选自定义上传策略，默认为 [DefaultUploadStrategy]
     */
    fun init(
        context: Application,
        uploader: EventUploader? = null,
        queue: EventQueue? = null,
        strategy: EventUploadStrategy? = null,
    ) {
        this.context = context
        this.uploader = uploader ?: RetryingUploader(HttpUploader())
        this.queue = queue ?: InMemoryEventQueue(uploader = this.uploader)
        this.uploadStrategy = strategy ?: DefaultUploadStrategy()
        ActivityPathTracker.init(context)
        EventDurationTracker.markColdStart()
    }

    /**
     * 添加事件拦截器。
     * 拦截器可在事件上报前修改事件内容。
     *
     * @param interceptor 待注册的事件拦截器
     */
    fun addInterceptor(interceptor: EventInterceptor) = interceptors.add(interceptor)

    /**
     * 添加事件插件。
     * 插件可在事件上报前后执行额外逻辑，例如日志、埋点统计。
     *
     * @param plugin 待注册的事件插件
     */
    fun addPlugin(plugin: EventPlugin) = plugins.add(plugin)

    /**
     * 强制刷新事件队列，将当前缓存的事件批量上报。
     */
    fun flushEvent() {
        scope.launch {
            uploader.uploadBatch(queue.snapshot())
        }
    }

    /**
     * 跟踪并处理事件。
     *
     * ## 流程
     * 1. 通过拦截器链修改事件
     * 2. 执行插件 onEventBeforeTrack
     * 3. 根据上传策略处理事件（立即上报或入队列）
     * 4. 执行插件 onEventAfterTrack
     *
     * @param event 待跟踪的事件
     */
    fun track(event: Event) {
        scope.launch {
            try {
                val chain = RealEventInterceptorChain(interceptors, 0, event)
                val finalEvent = chain.proceed() ?: return@launch
                plugins.forEach { it.onEventBeforeTrack(finalEvent) }
                val result = uploadStrategy.handle(finalEvent, queue, uploader)
                plugins.forEach { it.onEventAfterTrack(finalEvent, result) }
            } catch (t: Throwable) {
                println("Event tracking failed: $t")
            }
        }
    }
}
