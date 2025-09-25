package com.example.sampleview.eventtracker.store

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode
import com.example.sampleview.toBean
import com.example.sampleview.toJson

/**
 * 数据库事件实体，用于 Room 持久化存储事件。
 *
 * @property id 数据库自增主键
 * @property traceId 事件唯一标识
 * @property eventData 事件 JSON 数据
 * @property uploadMode 上传模式名称（UploadMode 枚举名称）
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val traceId: String,
    val eventData: String,
    val uploadMode: String,
) {
    companion object {
        /**
         * 将 [Event] 转换为 [EventEntity] 用于数据库存储
         *
         * @param event 待转换的事件对象
         * @return 对应的 [EventEntity]
         */
        fun fromEvent(event: Event): EventEntity {
            return EventEntity(traceId = event.traceId, eventData = event.toJson(), uploadMode = event.uploadMode.name)
        }

        /**
         * 将 [EventEntity] 转换为 [Event] 用于恢复事件
         *
         * @param entity 数据库中的事件实体
         * @return 对应的 [Event] 对象
         */
        fun toEvent(entity: EventEntity): Event {
            val event = entity.eventData.toBean<Event>()
            return event.copy(uploadMode = UploadMode.valueOf(entity.uploadMode), traceId = entity.traceId)
        }
    }
}
