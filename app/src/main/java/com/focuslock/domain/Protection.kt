package com.focuslock.domain

import android.graphics.Bitmap
import com.focuslock.data.AppSettingsEntity
import com.focuslock.data.ChallengeEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class ContentLevel { SAFE, SUGGESTIVE, SEXUAL, EXPLICIT }

/** Classificador visual local. Nenhum modelo vem embutido: sem modelo, `available` é falso e nada é bloqueado por imagem. */
interface ImageClassifier {
    val available: Boolean
    fun classify(bitmap: Bitmap): ContentLevel
}

class UnavailableImageClassifier : ImageClassifier {
    override val available = false
    override fun classify(bitmap: Bitmap) = ContentLevel.SAFE
}

object DomainClassifier {
    private val explicit = listOf("porn", "xxx", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "hentai", "brazzers", "spankbang", "rule34", "tube8")
    private val sexual = listOf("nsfw", "erotic", "nude", "chaturbate", "stripchat", "livejasmin", "onlyfans", "fansly", "camgirl")
    private val suggestive = listOf("playboy", "sexcam", "sexy")

    fun classify(host: String): ContentLevel {
        val h = host.lowercase()
        return when {
            explicit.any { h.contains(it) } -> ContentLevel.EXPLICIT
            sexual.any { h.contains(it) } -> ContentLevel.SEXUAL
            suggestive.any { h.contains(it) } -> ContentLevel.SUGGESTIVE
            else -> ContentLevel.SAFE
        }
    }

    fun shouldBlock(level: ContentLevel, minBlocked: ContentLevel): Boolean =
        level != ContentLevel.SAFE && level.ordinal >= minBlocked.ordinal
}

object Protection {
    fun endOf(ch: ChallengeEntity): LocalDateTime =
        LocalDate.parse(ch.startDate).plusDays(ch.totalDays.toLong()).atStartOfDay()

    fun isActive(ch: ChallengeEntity?, now: LocalDateTime = LocalDateTime.now()): Boolean =
        ch != null && now.isBefore(endOf(ch))

    fun hardcoreActive(st: AppSettingsEntity, now: LocalDateTime = LocalDateTime.now()): Boolean =
        st.hardcore && st.hardcoreEnd.isNotEmpty() && now.isBefore(LocalDateTime.parse(st.hardcoreEnd))

    fun remainingText(ch: ChallengeEntity, now: LocalDateTime = LocalDateTime.now()): String {
        val d = Duration.between(now, endOf(ch))
        if (d.isNegative) return "0 dias 00 horas 00 minutos"
        return "${d.toDays()} dias ${"%02d".format(d.toHours() % 24)} horas ${"%02d".format(d.toMinutes() % 60)} minutos"
    }

    fun domainBlocked(host: String, userDomains: Set<String>, adult: Boolean, minLevel: Int): Boolean {
        val h = host.lowercase().trimEnd('.')
        if (userDomains.any { h == it || h.endsWith(".$it") }) return true
        val min = ContentLevel.entries[minLevel.coerceIn(1, 3)]
        return adult && DomainClassifier.shouldBlock(DomainClassifier.classify(h), min)
    }
}

data class AchievementDef(val id: String, val title: String, val description: String)

object Achievements {
    val all = listOf(
        AchievementDef("first_day", "Primeiro passo", "Conclua o seu primeiro dia."),
        AchievementDef("week", "Semana firme", "7 dias seguidos."),
        AchievementDef("month", "Mês de ferro", "30 dias seguidos."),
        AchievementDef("goal", "Meta batida", "Conclua um objetivo."),
        AchievementDef("xp1000", "1.000 XP", "Alcance 1.000 XP."),
        AchievementDef("block", "Resistência", "Tenha um aplicativo ou site bloqueado.")
    )

    fun earned(best: Int, days: Int, goalsDone: Int, xp: Int, blocks: Int): Set<String> = buildSet {
        if (days >= 1) add("first_day")
        if (best >= 7) add("week")
        if (best >= 30) add("month")
        if (goalsDone >= 1) add("goal")
        if (xp >= 1000) add("xp1000")
        if (blocks >= 1) add("block")
    }
}
