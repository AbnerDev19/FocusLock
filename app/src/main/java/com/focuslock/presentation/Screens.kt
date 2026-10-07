package com.focuslock.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focuslock.data.GoalEntity
import com.focuslock.domain.Gamification
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val rows = Arrangement.spacedBy(16.dp)

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heatmap(days: Map<LocalDate, Int>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val end = today.plusDays((7 - today.dayOfWeek.value).toLong())
    val weeks = 15
    val start = end.minusDays((weeks * 7 - 1).toLong())
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (w in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (d in 0 until 7) {
                    val date = start.plusDays((w * 7 + d).toLong())
                    val xp = days[date]
                    val color = when {
                        date.isAfter(today) -> Color.Transparent
                        xp == null -> cs.surfaceVariant
                        xp < 300 -> cs.primary.copy(alpha = 0.45f)
                        xp < 1000 -> cs.primary.copy(alpha = 0.75f)
                        else -> cs.primary
                    }
                    val shape = RoundedCornerShape(3.dp)
                    Box(
                        Modifier.size(14.dp).clip(shape).background(color)
                            .then(if (date == selected) Modifier.border(1.5.dp, cs.onSurface, shape) else Modifier)
                            .clickable(enabled = !date.isAfter(today)) { onSelect(date) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(s: UiState, onCheckIn: () -> Unit, onCreate: (String, Int) -> Unit) {
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var dialog by remember { mutableStateOf(false) }
    val hour = LocalTime.now().hour
    val greet = when { hour < 12 -> "Bom dia"; hour < 18 -> "Boa tarde"; else -> "Boa noite" }
    val cs = MaterialTheme.colorScheme

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = rows) {
        item {
            Text("FOCUSLOCK", style = MaterialTheme.typography.labelLarge, color = cs.primary, fontWeight = FontWeight.Bold)
            Text("$greet!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Recupere o controle do seu tempo.", color = cs.onSurfaceVariant)
        }
        item {
            Panel {
                val ch = s.challenge
                if (ch == null) {
                    Text("Nenhum desafio ativo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { dialog = true }) { Text("Criar desafio") }
                } else {
                    Text(ch.name, style = MaterialTheme.typography.titleMedium)
                    Text("${s.streak} dias seguidos", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val frac = s.challengeDone.toFloat() / ch.totalDays
                    LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${s.challengeDone} / ${ch.totalDays} dias  ·  ${(frac * 100).toInt()}%  ·  faltam ${ch.totalDays - s.challengeDone}",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("XP", s.xp.toString(), Modifier.weight(1f))
                Stat("Nível", Gamification.level(s.xp).toString(), Modifier.weight(1f))
                Stat("Melhor", "${s.bestStreak} d", Modifier.weight(1f))
            }
        }
        item {
            Panel {
                Text("Calendário de disciplina", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Heatmap(s.days, selected) { selected = it }
                Spacer(Modifier.height(10.dp))
                val sel = selected
                Text(
                    when {
                        sel == null -> "Toque em um dia para ver os detalhes."
                        s.days[sel] != null -> "${sel.format(fmt)}  ·  +${s.days[sel]} XP  ·  dia concluído"
                        else -> "${sel.format(fmt)}  ·  sem check-in"
                    },
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
            }
        }
        item {
            Button(onClick = onCheckIn, enabled = !s.doneToday, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (s.doneToday) "Dia de hoje concluído" else "Concluir o dia de hoje")
            }
        }
    }

    if (dialog) {
        var name by remember { mutableStateOf("30 dias de foco") }
        var days by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Novo desafio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(
                        days, { days = it.filter(Char::isDigit) }, label = { Text("Duração em dias") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreate(name.ifBlank { "Meu desafio" }, (days.toIntOrNull() ?: 30).coerceAtLeast(1)); dialog = false
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun GoalsScreen(s: UiState, onAdd: (String) -> Unit, onComplete: (GoalEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = rows) {
        item { Text("Objetivos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.weight(1f), label = { Text("Novo objetivo") }, singleLine = true)
                Button(onClick = { if (title.isNotBlank()) { onAdd(title.trim()); title = "" } }) { Text("Adicionar") }
            }
        }
        items(s.goals, key = { it.id }) { g ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium)
                        Text("+${g.xp} XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (g.done) Text("Concluído", color = MaterialTheme.colorScheme.secondary)
                    else TextButton(onClick = { onComplete(g) }) { Text("Concluir") }
                }
            }
        }
    }
}

@Composable
fun ProgressScreen(s: UiState) {
    val cs = MaterialTheme.colorScheme
    val next = Gamification.nextLevelXp(s.xp)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = rows) {
        item { Text("Progresso", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            Panel {
                Text("Nível ${Gamification.level(s.xp)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { Gamification.levelProgress(s.xp) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Text(
                    if (next == null) "${s.xp} XP  ·  nível máximo" else "${s.xp} / $next XP  ·  faltam ${next - s.xp}",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Dias concluídos", s.days.size.toString(), Modifier.weight(1f))
                Stat("Sequência", "${s.streak} d", Modifier.weight(1f))
                Stat("Objetivos", s.goals.count { it.done }.toString(), Modifier.weight(1f))
            }
        }
        item {
            Panel {
                Text("Últimos 7 dias", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    for (i in 6 downTo 0) {
                        val done = LocalDate.now().minusDays(i.toLong()) in s.days
                        Box(Modifier.width(28.dp).height(if (done) 56.dp else 8.dp).clip(RoundedCornerShape(4.dp)).background(if (done) cs.primary else cs.surfaceVariant))
                    }
                }
            }
        }
    }
}

data class InfoItem(val title: String, val body: String)

@Composable
fun InfoScreen(title: String, items: List<InfoItem>) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = rows) {
        item { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        items(items) {
            Panel {
                Text(it.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(it.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
