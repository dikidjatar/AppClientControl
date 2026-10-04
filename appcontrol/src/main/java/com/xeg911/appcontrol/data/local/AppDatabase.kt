package com.xeg911.appcontrol.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deviceId: String,
    val deviceName: String,
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val source: String,
    val downloadUrl: String,
    val localUri: String,
    val receivedAt: Long,
    val savedAt: Long,
)

@Dao
interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<TransferHistoryEntity>>

    @Query("SELECT * FROM transfer_history WHERE deviceId = :deviceId ORDER BY savedAt DESC")
    fun observeByDevice(deviceId: String): Flow<List<TransferHistoryEntity>>

    @Query("SELECT * FROM transfer_history WHERE deviceId = :deviceId AND fileId = :fileId LIMIT 1")
    suspend fun find(deviceId: String, fileId: String): TransferHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferHistoryEntity): Long

    @Query("UPDATE transfer_history SET localUri = :localUri, savedAt = :savedAt WHERE id = :id")
    suspend fun updateLocation(id: Long, localUri: String, savedAt: Long)

    @Query("DELETE FROM transfer_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transfer_history")
    suspend fun clear()
}

@Database(entities = [TransferHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferHistoryDao

    companion object {
        const val NAME = "appcontrol.db"
    }
}
