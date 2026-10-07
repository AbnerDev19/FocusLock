package com.focuslock.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    InfoItem("Estado atual", "Nesta versão o app registra desafios, objetivos, XP e sequência. A proteção ativa ainda não está ligada."),
    InfoItem("Bloqueio de aplicativos", "Próxima etapa. Vai usar o Serviço de Acessibilidade do Android, que você precisa ativar manualmente em Ajustes do sistema."),
    InfoItem("Filtro de sites", "Etapa seguinte. Vai usar uma VPN local, só com filtro por domínio (DNS). O app não lê nem quebra o conteúdo criptografado."),
    InfoItem("Limite do Android", "Um app comum não consegue impedir de forma absoluta que o dono do aparelho o desative ou desinstale.")
)

private val settingsInfo = listOf(
    InfoItem("Privacidade", "Todos os dados (desafios, objetivos, XP e dias concluídos) ficam salvos apenas neste aparelho. O app não usa internet e não pede permissões."),
    InfoItem("Tema", "O app segue o modo claro ou escuro do seu sistema."),
    InfoItem("Versão", "FocusLock 0.1.0")
)

@Composable
fun FocusLockApp(vm: MainViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { t ->
                NavigationBarItem(
                    selected = current == t.route,
                    onClick = {
                        nav.navigate(t.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = { Text(t.label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { DashboardScreen(s, vm::checkIn, vm::createChallenge) }
            composable("goals") { GoalsScreen(s, vm::addGoal, vm::completeGoal) }
            composable("progress") { ProgressScreen(s) }
            composable("protect") { InfoScreen("Proteção", protectionInfo) }
            composable("settings") { InfoScreen("Ajustes", settingsInfo) }
        }
    }
}
