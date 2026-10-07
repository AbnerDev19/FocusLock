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
