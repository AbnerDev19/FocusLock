package com.focuslock.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.data.AppDatabase
import com.focuslock.data.ChallengeEntity
import com.focuslock.data.DailyProgressEntity
import com.focuslock.data.GoalEntity
import com.focuslock.data.XpTransactionEntity
import com.focuslock.domain.Gamification
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val xp: Int = 0,
    val days: Map<LocalDate, Int> = emptyMap(),
    val challenge: ChallengeEntity? = null,
    val challengeDone: Int = 0,
    val goals: List<GoalEntity> = emptyList(),
    val doneToday: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)

    val state: StateFlow<UiState> = combine(
        db.progressDao().all(), db.xpDao().total(), db.challengeDao().active(), db.goalDao().all()
    ) { progress, xp, challenge, goals ->
        val days = progress.associate { LocalDate.parse(it.date) to it.xp }
        val today = LocalDate.now()
        val done = if (challenge == null) 0 else {
            val start = LocalDate.parse(challenge.startDate)
            minOf(days.keys.count { !it.isBefore(start) }, challenge.totalDays)
        }
        UiState(
            streak = Gamification.currentStreak(days.keys, today),
            bestStreak = Gamification.bestStreak(days.keys),
            xp = xp, days = days, challenge = challenge, challengeDone = done,
            goals = goals, doneToday = today in days
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun checkIn() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val dates = state.value.days.keys
            if (today in dates) return@launch
            val gain = Gamification.dayXp(Gamification.currentStreak(dates + today, today))
            db.progressDao().upsert(DailyProgressEntity(today.toString(), gain))
            db.xpDao().insert(XpTransactionEntity(date = today.toString(), amount = gain, reason = "Dia concluído"))
        }
    }

    fun createChallenge(name: String, days: Int) {
        viewModelScope.launch {
            db.challengeDao().insert(ChallengeEntity(name = name, startDate = LocalDate.now().toString(), totalDays = days))
        }
    }

    fun addGoal(title: String) {
        viewModelScope.launch { db.goalDao().insert(GoalEntity(title = title)) }
    }

    fun completeGoal(goal: GoalEntity) {
        viewModelScope.launch {
            db.goalDao().update(goal.copy(done = true))
            db.xpDao().insert(XpTransactionEntity(date = LocalDate.now().toString(), amount = goal.xp, reason = "Objetivo: " + goal.title))
        }
    }
}
