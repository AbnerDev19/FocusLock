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
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    fun active(): Flow<ChallengeEntity?>

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

@Database(
    entities = [ChallengeEntity::class, GoalEntity::class, DailyProgressEntity::class, XpTransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun goalDao(): GoalDao
    abstract fun progressDao(): ProgressDao
    abstract fun xpDao(): XpDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "focuslock.db")
                .build().also { instance = it }
        }
    }
}
