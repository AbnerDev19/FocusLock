package com.focuslock.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.data.*
import com.focuslock.domain.Gamification
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val pad = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 28.dp)
private val sections = listOf("Metas", "Hábitos", "Prazos", "Estudo", "Mais")

@Composable
fun ProductivityScreen(s: UiState, vm: MainViewModel) {
    var section by remember { mutableStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) { delay(1000); timerSeconds++ }
    }

    LazyColumn(contentPadding = pad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Kicker("Rotina")
                H1("Seu dia")
                Lead("Metas, hábitos, prazos e estudo em um só lugar.")
            }
        }
        item {
            Column(Modifier.panel().padding(18.dp)) {
                StatRow(listOf(
                    s.xp.toString() to "XP",
                    Gamification.level(s.xp).toString() to "Nível",
                    s.goals.count { it.done }.toString() to "Metas"
                ))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sections.size) { i ->
                    Pill(sections[i], selected = section == i) { section = i }
                }
            }
        }

        when (section) {
            0 -> {
                item { SectionHeader("Metas", "Nova meta") { dialog = "goal" } }
                if (s.goals.isEmpty()) item { EmptyCard("Nenhuma meta ainda. Crie a primeira para ganhar XP.") }
                items(s.goals, key = { it.id }) { g ->
                    Row(Modifier.panel(14.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(g.title, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Row { TinyChip("+${g.xp} XP") }
                        }
                        if (g.done) TinyChip("Concluída", accent = true)
                        else LineButton("Concluir", { vm.completeGoal(g) }, Modifier.width(96.dp))
                    }
                }
            }

            1 -> {
                item { SectionHeader("Hábitos", "Novo hábito") { dialog = "habit" } }
                if (s.habits.isEmpty()) item { EmptyCard("Nenhum hábito ainda. Hábitos diários mantêm a sua sequência.") }
                items(s.habits, key = { it.id }) { h ->
                    val done = h.completedDate == LocalDate.now().toString()
                    Row(Modifier.panel(14.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(h.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("🔥 ${h.streak} dia(s)", color = C.Faint, fontSize = 12.sp)
                        }
                        LineButton(if (done) "Feito" else "+10 XP", { if (!done) vm.toggleHabit(h) }, Modifier.width(96.dp))
                    }
                }
            }

            2 -> {
                item { SectionHeader("Prazos", "Nova atividade") { dialog = "activity" } }
                if (s.activities.isEmpty()) item { EmptyCard("Nenhuma atividade com prazo.") }
                items(s.activities, key = { it.id }) { a ->
                    val overdue = !a.completed && !a.failed && a.dueAt < System.currentTimeMillis()
                    val due = DateTimeFormatter.ofPattern("dd/MM HH:mm").format(Instant.ofEpochMilli(a.dueAt).atZone(ZoneId.systemDefault()))
                    Row(Modifier.panel(14.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(a.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("+${a.xp} XP  ·  vence em $due", color = if (overdue) C.Accent2 else C.Faint, fontSize = 12.sp)
                        }
                        if (a.completed) TinyChip("Concluída", accent = true)
                        else if (a.failed) TinyChip("Falhou")
                        else LineButton("Concluir", { vm.completeActivity(a) }, Modifier.width(96.dp))
                    }
                }
            }

            3 -> {
                item { SectionHeader("Sala de estudos", "Nova matéria") { dialog = "subject" } }
                item {
                    Column(Modifier.panel(14.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(formatTimer(timerSeconds), color = C.Text, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text("Matéria: ${selectedSubject?.name ?: "Estudo geral"}", color = C.Faint, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!timerRunning) LineButton("Iniciar", { timerRunning = true }, Modifier.weight(1f))
                            else LineButton("Pausar", { timerRunning = false }, Modifier.weight(1f))
                            LineButton("Finalizar + XP", {
                                timerRunning = false
                                val min = timerSeconds / 60
                                if (min > 0) vm.addStudy(min, selectedSubject)
                                timerSeconds = 0
                            }, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Escolher matéria", color = C.Dim, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Pill("Geral", selected = selectedSubject == null) { selectedSubject = null } }
                            items(s.subjects, key = { it.id }) { sub ->
                                Pill(sub.name, selected = selectedSubject?.id == sub.id) { selectedSubject = sub }
                            }
                        }
                    }
                }
                item { LineButton("Registrar tempo manualmente", { dialog = "study" }) }
            }

            else -> {
                item { SectionHeader("Atributos", "Novo atributo") { dialog = "attribute" } }
                if (s.attributes.isEmpty()) item { EmptyCard("Atributos medem a sua evolução em áreas como força, mente ou foco.") }
                items(s.attributes, key = { it.id }) { a ->
                    Row(Modifier.panel(14.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(a.name, color = C.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        TinyChip("Nível ${a.level}  ·  ${a.xp} XP")
                    }
                }
                item { Column(Modifier.padding(top = 10.dp)) { SectionHeader("Histórico", "Registrar") { dialog = "log" } } }
                if (s.history.isEmpty()) item { EmptyCard("O histórico recente aparece aqui.") }
                else {
                    item {
                        Column(Modifier.panel(14.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            s.history.take(15).forEach { h ->
                                val time = Instant.ofEpochMilli(h.timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(time, color = C.Faint, fontSize = 12.sp)
                                    Text(h.text, color = C.Dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item { LineButton("Limpar histórico", vm::clearHistory) }
                }
            }
        }
    }

    when (dialog) {
        "goal" -> SimpleTextDialog("Nova meta", "Nome", "50", { name, xp -> vm.addRoutineGoal(name, xp); dialog = null }, onCancel = { dialog = null })
        "habit" -> SimpleTextDialog("Novo hábito", "Nome", "", { name, _ -> vm.addHabit(name, null); dialog = null }, false, onCancel = { dialog = null })
        "attribute" -> SimpleTextDialog("Novo atributo", "Nome", "", { name, _ -> vm.addAttribute(name); dialog = null }, false, onCancel = { dialog = null })
        "subject" -> SimpleTextDialog("Nova matéria", "Nome", "", { name, _ -> vm.addSubject(name); dialog = null }, false, onCancel = { dialog = null })
        "activity" -> SimpleTextDialog("Nova atividade", "Nome", "60", { name, xp -> vm.addActivity(name, System.currentTimeMillis() + 60 * 60_000L, xp.coerceAtLeast(1), null); dialog = null }, onCancel = { dialog = null })
        "study" -> SimpleTextDialog("Tempo manual", "Minutos", "", { txt, _ -> vm.addStudy(txt.filter(Char::isDigit).toIntOrNull() ?: 0, selectedSubject); dialog = null }, false, onCancel = { dialog = null })
        "log" -> SimpleTextDialog("Registrar ação", "O que você fez?", "", { name, _ -> vm.logAction(name); dialog = null }, false, onCancel = { dialog = null })
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) { SectionTitle(title) }
        LineButton(action, onAction, Modifier.width(124.dp))
    }
}

@Composable
private fun EmptyCard(text: String) {
    Column(Modifier.panel(14.dp).padding(18.dp)) {
        Text(text, color = C.Faint, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .background(if (selected) C.Accent.copy(alpha = 0.14f) else C.Surface)
            .border(1.dp, if (selected) C.Accent else C.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) C.Text else C.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun formatTimer(sec: Int): String = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)

@Composable
private fun SimpleTextDialog(title: String, label: String, secondDefault: String, onSave: (String, Int) -> Unit, hasNumber: Boolean = true, numeric: Boolean = false, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var number by remember { mutableStateOf(secondDefault) }
    AlertDialog(
        onDismissRequest = onCancel, containerColor = C.Surface2, titleContentColor = C.Text, textContentColor = C.Dim,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(9.dp), colors = fieldColors())
                if (hasNumber) OutlinedTextField(number, { number = it.filter(Char::isDigit) }, label = { Text(if (numeric) "Minutos" else "XP") }, singleLine = true, shape = RoundedCornerShape(9.dp), colors = fieldColors())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text, number.toIntOrNull() ?: 0) }, colors = ButtonDefaults.textButtonColors(contentColor = C.Accent2)) { Text("Salvar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = C.Dim)) { Text("Cancelar") } }
    )
}
