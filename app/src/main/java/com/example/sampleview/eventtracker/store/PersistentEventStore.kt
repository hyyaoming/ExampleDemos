package com.example.sampleview.eventtracker.store

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode

/**
 * **事件持久化存储接口**
 *
 * 定义事件在磁盘、数据库或其他长期介质中的存取规范。
 * 主要用于在网络不可用、应用退出或异常情况下，保证事件数据不丢失。
 *
 * ### 特性
 * - 所有方法均为 `suspend`，便于在 IO 线程中异步执行；
 * - 要求实现类具备线程安全；
 * - 支持按 [UploadMode] 分类恢复事件。
 *
 * ### 常见实现
 * - [PersistentEventDBStore]：基于数据库的存储实现；
 * - 文件存储实现：将事件序列化为文件；
 * - 自定义存储：如共享偏好、云端缓存等。
 */
interface PersistentEventStore {

    /**
     * 批量持久化事件。
     *
     * 使用场景：
     * - 新事件产生但未能立即上传时；
     * - 应用进入后台或退出前需要落盘时；
     * - 网络不可用，需要先缓存事件。
     *
     * @param events 待持久化的事件列表
     */
    suspend fun persist(events: List<Event>)

    /**
     * 删除已持久化的事件。
     *
     * 使用场景：
     * - 事件已成功上传；
     * - 需要清理过期或无效事件；
     * - 控制存储空间占用。
     *
     * @param events 待删除的事件列表
     */
    suspend fun remove(events: List<Event>)

    /**
     * 从持久化存储中恢复事件。
     *
     * 使用场景：
     * - 应用启动时恢复上次未上传的事件；
     * - 按不同的 [UploadMode] 恢复对应类别的事件；
     * - 实现断点续传，保证数据完整性。
     *
     * @param uploadModel 上传模式（即时上传、批量上传、延迟上传等）
     * @return 已恢复的事件列表，若无可恢复事件则返回空列表
     */
    suspend fun restore(uploadModel: UploadMode): List<Event>
}
