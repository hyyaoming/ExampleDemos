package com.example.sampleview.eventtracker

import com.example.sampleview.eventtracker.EventTrackerConfig.Companion.DEFAULT
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.plugin.EventPlugin
import com.example.sampleview.eventtracker.store.PersistentEventDBStore
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.HttpUploader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * **EventTrackerConfig**
 *
 * 全局事件跟踪器配置类，用于管理事件上传、队列、全局属性、拦截器和插件。
 *
 * ### 功能
 * 1. 配置事件上传器及队列相关参数
 * 2. 设置全局事件属性（每个事件自动附加）
 * 3. 注册事件拦截器链 [EventInterceptor]
 * 4. 注册事件插件链 [EventPlugin]
 * 5. 配置持久化事件存储 [PersistentEventStore]
 *
 * 配置通过 [Builder] 链式构建，可自定义各项参数，也可使用默认配置 [DEFAULT]。
 *
 * @property uploaderConfig 上传器及队列相关配置
 * @property globalProperties 全局事件属性，每个事件都会附加
 * @property interceptors 已注册的事件拦截器列表
 * @property plugins 已注册的事件插件列表
 * @property eventStore 持久化存储，默认使用 [PersistentEventDBStore]
 */
class EventTrackerConfig private constructor(
    val uploaderConfig: UploaderConfig,
    val globalProperties: Map<String, Any>,
    val interceptors: List<EventInterceptor>,
    val plugins: List<EventPlugin>,
    val eventStore: PersistentEventStore,
) {

    /**
     * **UploaderConfig**
     *
     * 配置事件上传器和队列参数。
     *
     * @property batchSize 批量上传事件阈值，达到该数量触发上传，默认 2
     * @property queueCapacity 内存队列最大容量，超过后新事件丢弃，默认 1000
     * @property maxRetry 上传失败时最大重试次数，默认 3
     * @property initialDelay 上传失败后初始重试延迟，默认 1 秒
     * @property maxDelay 上传失败后最大重试延迟，默认 5 秒
     * @property uploader 自定义事件上传器，默认使用 [HttpUploader]
     */
    data class UploaderConfig(
        val batchSize: Int = 2,
        val queueCapacity: Int = 1000,
        val maxRetry: Int = 3,
        val initialDelay: Duration = 1.seconds,
        val maxDelay: Duration = 5.seconds,
        var uploader: EventUploader = HttpUploader(),
    )

    /**
     * **Builder**
     *
     * 用于链式构建 [EventTrackerConfig] 对象。
     */
    class Builder {
        private var uploaderConfig: UploaderConfig = UploaderConfig()
        private val globalProperties: MutableMap<String, Any> = mutableMapOf()
        private val interceptors: MutableList<EventInterceptor> = mutableListOf()
        private val plugins: MutableList<EventPlugin> = mutableListOf()
        private var eventStore: PersistentEventStore = PersistentEventDBStore()

        /** 设置批量上传阈值 */
        fun batchSize(batchSize: Int) = apply {
            uploaderConfig = uploaderConfig.copy(batchSize = batchSize)
        }

        /** 设置内存队列最大容量 */
        fun queueCapacity(capacity: Int) = apply {
            uploaderConfig = uploaderConfig.copy(queueCapacity = capacity)
        }

        /** 设置上传失败最大重试次数 */
        fun uploadRetryCount(maxRetry: Int) = apply {
            uploaderConfig = uploaderConfig.copy(maxRetry = maxRetry)
        }

        /** 设置上传失败初始重试延迟 */
        fun uploadInitialDelay(delay: Duration) = apply {
            uploaderConfig = uploaderConfig.copy(initialDelay = delay)
        }

        /** 设置上传失败最大重试延迟 */
        fun uploadMaxDelay(delay: Duration) = apply {
            uploaderConfig = uploaderConfig.copy(maxDelay = delay)
        }

        /** 设置自定义上传器 */
        fun uploader(uploader: EventUploader) = apply {
            uploaderConfig = uploaderConfig.copy(uploader = uploader)
        }

        /** 设置持久化存储实现 */
        fun eventStore(store: PersistentEventStore) = apply {
            this.eventStore = store
        }

        /** 添加单个全局属性 */
        fun globalProperty(key: String, value: Any) = apply {
            globalProperties[key] = value
        }

        /** 添加多个全局属性 */
        fun globalProperties(props: Map<String, Any>) = apply {
            globalProperties.putAll(props)
        }

        /** 添加事件拦截器 */
        fun addInterceptor(interceptor: EventInterceptor) = apply {
            interceptors.add(interceptor)
        }

        /** 添加事件插件 */
        fun addPlugin(plugin: EventPlugin) = apply {
            plugins.add(plugin)
        }

        /** 构建最终的 [EventTrackerConfig] 对象 */
        fun build(): EventTrackerConfig = EventTrackerConfig(
            uploaderConfig = uploaderConfig,
            globalProperties = globalProperties.toMap(),
            interceptors = interceptors.toList(),
            plugins = plugins.toList(),
            eventStore = eventStore
        )
    }

    companion object {
        /** 默认配置对象，可直接使用 [Builder] 构建的默认配置 */
        val DEFAULT: EventTrackerConfig by lazy { Builder().build() }
    }
}
