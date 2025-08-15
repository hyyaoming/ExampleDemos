package com.example.sampleview.log

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
)

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntryEntity>)

    @Query("DELETE FROM log_entries WHERE timestamp <= :beforeTimestamp")
    suspend fun deleteLogsBefore(beforeTimestamp: Long)

    @Query("SELECT * FROM log_entries ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getLogs(limit: Int): List<LogEntryEntity>
}

@Database(entities = [LogEntryEntity::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var instance: LogDatabase? = null
        fun getInstance(context: android.content.Context): LogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "log_db"
                ).build().also { instance = it }
            }
    }
}
