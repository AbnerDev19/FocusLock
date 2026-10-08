package com.focuslock.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    fun active(): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    suspend fun activeNow(): ChallengeEntity?

    @Insert
    suspend fun insert(c: ChallengeEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY done, id DESC")
    fun all(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(g: GoalEntity)

    @Update
    suspend fun update(g: GoalEntity)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM daily_progress ORDER BY date")
    fun all(): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress")
    suspend fun allNow(): List<DailyProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: DailyProgressEntity)
}

@Dao
interface XpDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_transactions")
    fun total(): Flow<Int>

    @Insert
    suspend fun insert(t: XpTransactionEntity)
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY label")
    fun all(): Flow<List<BlockedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(a: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface BlockedDomainDao {
    @Query("SELECT * FROM blocked_domains ORDER BY domain")
    fun all(): Flow<List<BlockedDomainEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(d: BlockedDomainEntity)

    @Query("DELETE FROM blocked_domains WHERE domain = :d")
    suspend fun delete(d: String)
}

@Dao
interface BlockLogDao {
    @Query("SELECT * FROM block_log")
    fun all(): Flow<List<BlockLogEntity>>

    @Query("UPDATE block_log SET count = count + 1 WHERE date = :d")
    suspend fun inc(d: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: BlockLogEntity)
}

suspend fun BlockLogDao.record() {
    val d = LocalDate.now().toString()
    if (inc(d) == 0) insert(BlockLogEntity(d, 1))
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getNow(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: AppSettingsEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun all(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(a: AchievementEntity)
}

@Database(
    entities = [
        ChallengeEntity::class, GoalEntity::class, DailyProgressEntity::class, XpTransactionEntity::class,
        BlockedAppEntity::class, BlockedDomainEntity::class, BlockLogEntity::class,
        AchievementEntity::class, AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun goalDao(): GoalDao
    abstract fun progressDao(): ProgressDao
    abstract fun xpDao(): XpDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun blockLogDao(): BlockLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "focuslock.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
