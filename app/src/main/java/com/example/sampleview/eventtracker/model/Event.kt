package com.example.sampleview.eventtracker.model

/**
 * 表示一个事件对象，用于埋点或上报。
 *
 * @property eventId 事件唯一标识
 * @property properties 事件相关的属性集合，可动态添加任意键值对
 * @property timestamp 事件创建时间，毫秒级时间戳
 * @property uploadMode 事件的上报策略，例如立即上传或批量上传
 */
data class Event(
    val eventId: String,
    val properties: MutableMap<String, Any>,
    val timestamp: Long,
    val uploadMode: UploadMode,
) {

    /**
     * [Event] 的构建器，用于链式构造事件对象。
     *
     * @param eventId 事件唯一标识
     */
    class Builder(private val eventId: String) {

        /**
         * 事件属性集合，用于存储事件的动态键值对。
         * 最终会复制到 Event.properties 中。
         */
        private val properties = mutableMapOf<String, Any>()

        /**
         * 事件上报模式，默认使用 [UploadMode.IMMEDIATE]，表示立即上传。
         * 可通过 [uploadMode] 方法修改。
         */
        private var uploadMode: UploadMode = UploadMode.IMMEDIATE

        /**
         * 事件时间戳，默认使用 Builder 创建时的系统当前时间（毫秒）。
         * 可通过 [timestamp] 方法手动设置。
         */
        private var timestamp: Long = System.currentTimeMillis()

        /**
         * 添加单个事件属性。
         *
         * @param key 属性名
         * @param value 属性值
         * @return 返回 Builder 本身以支持链式调用
         */
        fun property(key: String, value: Any) = apply { properties[key] = value }

        /**
         * 批量添加事件属性。
         *
         * @param props 属性键值对集合
         * @return 返回 Builder 本身以支持链式调用
         */
        fun properties(props: Map<String, Any>) = apply { properties.putAll(props) }

        /**
         * 设置事件的上报模式。
         *
         * @param mode [UploadMode] 类型
         * @return 返回 Builder 本身以支持链式调用
         */
        fun uploadMode(mode: UploadMode) = apply { this.uploadMode = mode }

        /**
         * 设置事件时间戳，默认使用当前时间。
         *
         * @param time 毫秒级时间戳
         * @return 返回 Builder 本身以支持链式调用
         */
        fun timestamp(time: Long) = apply { this.timestamp = time }

        /**
         * 构建 [Event] 对象。
         *
         * @return 创建好的 [Event] 实例
         */
        fun build() = Event(eventId, properties.toMutableMap(), timestamp, uploadMode)
    }
}
