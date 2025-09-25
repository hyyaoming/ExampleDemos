package com.example.sampleview.eventtracker.store

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 数据库，用于存储事件实体
 */
@Database(entities = [EventEntity::class], version = 1)
abstract class EventDatabase : RoomDatabase() {

    /**
     * 获取事件 DAO
     */
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        /**
         * 获取单例数据库实例
         *
         * @param context Application 上下文
         * @return 单例 [EventDatabase] 实例
         */
        fun getInstance(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    EventDatabase::class.java,
                    "event_tracker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
