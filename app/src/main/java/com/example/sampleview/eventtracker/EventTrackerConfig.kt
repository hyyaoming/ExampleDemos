package com.example.sampleview.eventtracker

import com.example.sampleview.eventtracker.EventTrackerConfig.Companion.DEFAULT
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.plugin.EventPlugin
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.HttpUploader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * EventTrackerConfig 是全局事件跟踪器的配置类。
 *
 * 用于统一管理：
 * 1. 上传器及队列相关参数（批量大小、队列容量、重试策略等）
 * 2. 全局事件属性（每个事件自动附加的属性）
 * 3. 事件拦截器链（EventInterceptor）
 * 4. 事件插件链（EventPlugin）
 *
 * 配置通过 Builder 模式构建，可自定义各项参数，也提供默认配置 [DEFAULT]。
 *
 * @property uploaderConfig 上传器和队列相关的配置
 * @property globalProperties 全局属性，会自动附加到每个事件
 * @property interceptors 已注册的事件拦截器列表
 * @property plugins 已注册的事件插件列表
 */
class EventTrackerConfig private constructor(
    val uploaderConfig: UploaderConfig,
    val globalProperties: Map<String, Any>,
    val interceptors: List<EventInterceptor>,
    val plugins: List<EventPlugin>,
) {

    /**
     * 上传器及队列相关配置。
     *
     * @property batchSize 批量上传事件的数量阈值，默认 50
     * @property queueCapacity 内存队列容量，默认 1000
     * @property maxRetry 上传失败时最大重试次数，默认 3
     * @property initialDelay 上传失败后初始重试延迟，默认 1 秒
     * @property maxDelay 上传失败后最大重试延迟，默认 5 秒
     * @property uploader 可选自定义 EventUploader，若为空将使用默认实现
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
     * Builder 用于链式构建 [EventTrackerConfig] 对象。
     */
    class Builder {
        private var uploaderConfig: UploaderConfig = UploaderConfig()
        private val globalProperties: MutableMap<String, Any> = mutableMapOf()
        private val interceptors: MutableList<EventInterceptor> = mutableListOf()
        private val plugins: MutableList<EventPlugin> = mutableListOf()

        // --- 上传器配置 ---
        fun batchSize(batchSize: Int) = apply { this.uploaderConfig = uploaderConfig.copy(batchSize = batchSize) }
        fun uploadRetryCount(maxRetry: Int) = apply { this.uploaderConfig = uploaderConfig.copy(maxRetry = maxRetry) }
        fun uploadInitialDelay(delay: Duration) = apply { this.uploaderConfig = uploaderConfig.copy(initialDelay = delay) }
        fun uploadMaxDelay(delay: Duration) = apply { this.uploaderConfig = uploaderConfig.copy(maxDelay = delay) }
        fun uploader(uploader: EventUploader) = apply { this.uploaderConfig = uploaderConfig.copy(uploader = uploader) }

        // --- 全局属性 ---
        fun globalProperty(key: String, value: Any) = apply { this.globalProperties[key] = value }
        fun globalProperties(props: Map<String, Any>) = apply { this.globalProperties.putAll(props) }

        // --- 拦截器 / 插件 ---
        fun addInterceptor(interceptor: EventInterceptor) = apply { interceptors.add(interceptor) }
        fun addPlugin(plugin: EventPlugin) = apply { plugins.add(plugin) }

        /** 构建最终的 [EventTrackerConfig] 对象 */
        fun build(): EventTrackerConfig = EventTrackerConfig(
            uploaderConfig = uploaderConfig,
            globalProperties = globalProperties.toMap(),
            interceptors = interceptors.toList(),
            plugins = plugins.toList()
        )
    }

    companion object {
        /** 默认配置对象，可直接使用 */
        val DEFAULT: EventTrackerConfig by lazy { Builder().build() }
    }
}
