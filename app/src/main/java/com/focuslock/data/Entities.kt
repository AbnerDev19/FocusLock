package com.focuslock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: String,
    val totalDays: Int
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Geral",
    val xp: Int = 50,
    val done: Boolean = false
)

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val date: String,
    val xp: Int,
    val blocksTriggered: Int = 0
)

@Entity(tableName = "xp_transactions")
data class XpTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amount: Int,
    val reason: String
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(@PrimaryKey val packageName: String, val label: String)

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(@PrimaryKey val domain: String)

@Entity(tableName = "block_log")
data class BlockLogEntity(@PrimaryKey val date: String, val count: Int)

@Entity(tableName = "achievements")
data class AchievementEntity(@PrimaryKey val id: String, val date: String)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val onboarded: Boolean = false,
    val protectApps: Boolean = true,
    val protectSites: Boolean = true,
    val adultFilter: Boolean = true,
    val sensitivity: Int = 2,
    val notifications: Boolean = true,
    val hardcore: Boolean = false,
    val hardcoreEnd: String = ""
)
