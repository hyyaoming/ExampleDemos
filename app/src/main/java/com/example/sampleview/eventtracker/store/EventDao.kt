package com.example.sampleview.eventtracker.store

import EventEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * **事件数据库 DAO 接口**
 *
 * 用于通过 Room 对事件表 (`events`) 执行增删查操作。
 * 提供事件持久化存储、按 traceId 批量删除以及按上传模式查询功能。
 */
@Dao
interface EventDao {

    /**
     * 插入事件列表
     *
     * @param events 待插入的事件实体列表
     * @return 返回生成的主键数组，长度等于插入成功的条数
     */
    @Insert
    suspend fun insert(events: List<EventEntity>): LongArray

    /**
     * 根据 traceId 批量删除事件
     *
     * @param traceIds 待删除事件的 traceId 列表
     * @return 返回删除的行数
     */
    @Query("DELETE FROM events WHERE traceId IN (:traceIds)")
    suspend fun deleteByTraceIds(traceIds: List<String>): Int

    /**
     * 根据上传模式查询事件列表，并按插入顺序升序排列。
     *
     * 使用场景：
     * - 恢复指定上传模式的事件时，需要保证事件按插入顺序处理，避免乱序上传。
     *
     * @param uploadMode 上传模式名称（UploadMode 枚举的 name）
     * @return 对应上传模式下的事件实体列表，按数据库自增主键 `id` 升序排列
     */
    @Query("SELECT * FROM events WHERE uploadMode = :uploadMode ORDER BY id ASC")
    suspend fun queryByUploadMode(uploadMode: String): List<EventEntity>
}
