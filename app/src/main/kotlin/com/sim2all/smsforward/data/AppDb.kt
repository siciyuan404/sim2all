package com.sim2all.smsforward.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 待发送 / 已发送的短信记录。同一张表用 status 区分状态。
 *
 * - PENDING:  待发送（已入库，等待 ForwardService 取走）
 * - SENDING:  发送中
 * - SENT:     已发送成功
 * - FAILED:   重试次数用尽仍失败
 */
@Entity(tableName = "pending_sms")
data class PendingSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,           // 短信到达时间（毫秒）
    val toAddress: String,          // 收件邮箱（快照，避免后续配置变更影响）
    val status: String = STATUS_PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null,
    val sentAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENDING = "SENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
    }
}

@Dao
interface PendingSmsDao {

    @Insert
    suspend fun insert(item: PendingSms): Long

    @Update
    suspend fun update(item: PendingSms)

    @Query("SELECT * FROM pending_sms WHERE status = :status ORDER BY receivedAt ASC LIMIT :limit")
    suspend fun listByStatus(status: String, limit: Int = 50): List<PendingSms>

    @Query("SELECT * FROM pending_sms WHERE status IN ('PENDING','SENDING','FAILED') ORDER BY receivedAt ASC")
    fun observePending(): Flow<List<PendingSms>>

    @Query("SELECT * FROM pending_sms ORDER BY receivedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<PendingSms>>

    @Query("SELECT COUNT(*) FROM pending_sms WHERE status IN ('PENDING','SENDING','FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM pending_sms WHERE status = 'SENT' AND sentAt < :before")
    suspend fun purgeSentBefore(before: Long): Int

    @Query("DELETE FROM pending_sms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_sms SET status = 'PENDING' WHERE status = 'SENDING'")
    suspend fun resetStuckSending()
}

@Database(entities = [PendingSms::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun pendingSmsDao(): PendingSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "sms_forward.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
