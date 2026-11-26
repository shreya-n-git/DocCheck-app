package com.doccheck.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun observeAllScans(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: String): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(entity: ScanResultEntity)

    @Query("SELECT COUNT(*) FROM scan_results")
    fun observeScanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE tamperScore >= 50")
    fun observeTamperedCount(): Flow<Int>

    @Query("DELETE FROM scan_results")
    suspend fun clearAllScans()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Database(
    entities = [ScanResultEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class DocCheckDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun chatDao(): ChatDao
}

