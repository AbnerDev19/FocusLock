package com.focuslock.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Início", Icons.Filled.Home),
    Tab("goals", "Objetivos", Icons.Filled.CheckCircle),
    Tab("progress", "Progresso", Icons.Filled.Star),
    Tab("protect", "Proteção", Icons.Filled.Lock),
    Tab("settings", "Ajustes", Icons.Filled.Settings)
)

private val protectionInfo = listOf(
    InfoItem("Estado atual", "Esta versão registra desafios, objetivos, XP e sequência. A proteção ativa ainda não está ligada."),
    InfoItem("Bloqueio de aplicativos", "Vai usar o Serviço de Acessibilidade do Android, que você ativa manualmente nos ajustes do sistema.", "Em breve"),
    InfoItem("Filtro de sites", "Vai usar uma VPN local, só com filtro por domínio (DNS). O app não lê nem quebra conteúdo criptografado.", "Em breve"),
    InfoItem("Limite do Android", "Um app comum não consegue impedir de forma absoluta que o dono do aparelho o desative ou desinstale.")
)

private val settingsInfo = listOf(
    InfoItem("Privacidade", "Desafios, objetivos, XP e dias concluídos ficam salvos apenas neste aparelho. O app não usa internet e não pede permissões."),
    InfoItem("Aparência", "O FocusLock usa um tema escuro único, pensado para telas pequenas."),
    InfoItem("Versão", "FocusLock 0.2.0")
)

@Composable
fun FocusLockApp(vm: MainViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    Column(
        Modifier.fillMaxSize().drawBehind {
            drawRect(C.Bg)
            drawRect(Brush.radialGradient(listOf(C.Accent.copy(alpha = 0.10f), Color.Transparent), Offset(size.width * 0.15f, 0f), size.width))
            drawRect(Brush.radialGradient(listOf(Color(0xFF6496FF).copy(alpha = 0.06f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.2f), size.width * 0.9f))
        }
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text("FocusLock", color = C.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em)
                Text(".", color = C.Accent2, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (s.challenge != null) C.Green else C.Faint))
                Spacer(Modifier.width(8.dp))
                Text(if (s.challenge != null) "Desafio ativo" else "Sem desafio", color = C.Dim, fontSize = 12.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

        NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
            composable("home") { DashboardScreen(s, vm::checkIn, vm::createChallenge) }
            composable("goals") { GoalsScreen(s, vm::addGoal, vm::completeGoal) }
            composable("progress") { ProgressScreen(s) }
            composable("protect") { InfoScreen("Proteção", "Proteção", "O que o FocusLock vai proteger e o que o Android permite.", protectionInfo) }
            composable("settings") { InfoScreen("Ajustes", "Ajustes", "Privacidade e informações do aplicativo.", settingsInfo) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
        Row(Modifier.fillMaxWidth().background(C.Bg.copy(alpha = 0.88f)).height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { t ->
                val on = current == t.route
                Column(
                    Modifier.weight(1f).padding(horizontal = 3.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (on) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                        .clickable {
                            nav.navigate(t.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(t.icon, contentDescription = t.label, tint = if (on) C.Text else C.Faint, modifier = Modifier.size(20.dp))
                    Text(t.label, color = if (on) C.Text else C.Faint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}
