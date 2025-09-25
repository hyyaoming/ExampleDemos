package com.example.sampleview.eventtracker.store

import com.example.sampleview.eventtracker.EventTracker
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于数据库的事件持久化存储实现
 *
 * 提供事件的持久化保存、删除和恢复接口。
 * 用于在应用退出、崩溃或网络不可用时保证事件不丢失。
 *
 * ### 特性
 * - 支持将事件持久化到数据库；
 * - 上传成功后可删除对应事件；
 * - 可按 [UploadMode] 恢复事件。
 */
class PersistentEventDBStore() : PersistentEventStore {

    /** Room DAO 对象，用于操作事件数据库 */
    private val eventDao = EventDatabase.getInstance(EventTracker.context).eventDao()

    /**
     * 将事件持久化保存到数据库
     *
     * 使用场景：
     * - 事件入队但未上传时；
     * - 应用即将退出或进入后台时；
     * - 网络不可用时，先落盘以防丢失。
     *
     * @param events 待持久化的事件列表
     */
    override suspend fun persist(events: List<Event>) {
        withContext(Dispatchers.IO) {
            val entities = events.map { EventEntity.fromEvent(it) }
            val insertedIds = eventDao.insert(entities)
            val successCount = insertedIds.count { it > 0 }
            TrackerLogger.logger.log("数据库持久化：成功保存 $successCount/${events.size} 条事件")
        }
    }

    /**
     * 从数据库中删除指定事件
     *
     * 使用场景：
     * - 事件已成功上传；
     * - 需要清理已处理或过期的事件。
     *
     * @param events 待删除的事件列表
     */
    override suspend fun remove(events: List<Event>) {
        withContext(Dispatchers.IO) {
            val traceIds = events.map { it.traceId }
            val deletedCount = eventDao.deleteByTraceIds(traceIds)
            TrackerLogger.logger.log("数据库删除：成功删除 $deletedCount/${traceIds.size} 条事件")
        }
    }

    /**
     * 从数据库恢复指定上传模式的事件
     *
     * 使用场景：
     * - 应用启动时恢复上次未上传的事件；
     * - 根据不同的 [UploadMode] 恢复对应模式下的事件。
     *
     * @param uploadModel 上传模式（如即时上传、批量上传、延迟上传等）
     * @return 对应上传模式下恢复的事件列表，如果没有事件则返回空列表
     */
    override suspend fun restore(uploadModel: UploadMode): List<Event> {
        return withContext(Dispatchers.IO) {
            val entities = eventDao.queryByUploadMode(uploadModel.name)
            val restoredEvents = entities.map { EventEntity.toEvent(it) }
            TrackerLogger.logger.log("数据库恢复：为上传模式 ${uploadModel.name} 恢复了 ${restoredEvents.size} 条事件")
            restoredEvents
        }
    }
}
