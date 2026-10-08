# FocusLock

"Recupere o controle do seu tempo." App Android de autocontrole e disciplina digital.

## Funcionalidades (versão 1.0.0)
- Onboarding em 4 passos, nome e primeiro desafio
- Desafios com sequência, melhor sequência, XP, nível, progresso e dias restantes
- Calendário de disciplina (15 semanas) com detalhe do dia: XP e bloqueios acionados
- Objetivos com XP e conquistas
- Bloqueio de aplicativos (Serviço de Acessibilidade) com tela de bloqueio e contagem regressiva do desafio
- Filtro de sites por DNS (VPN local) com lista própria e filtro de sites adultos em 3 níveis de sensibilidade
- Modo Hardcore (trava as escolhas até o fim do desafio) e administrador do dispositivo opcional
- Lembretes (no máximo 1 por dia) e aviso ao concluir o dia
- Visual escuro no mesmo estilo do portfólio

## Tecnologias
Kotlin, Jetpack Compose, Material 3, MVVM, Room, Navigation Compose, WorkManager, AccessibilityService, VpnService, DevicePolicyManager.

## Arquitetura
- `data/`: entidades e DAOs do Room (desafios, objetivos, progresso, XP, apps e sites bloqueados, bloqueios, conquistas, configurações)
- `domain/`: XP, níveis, sequência, regras de proteção e classificador de domínios
- `presentation/`: tema, componentes, telas, navegação e ViewModel
- `services/`: AppBlockingService, BlockActivity, FocusVpnService, FocusAdminReceiver, ReminderWorker, Notifier

## Como gerar o APK com GitHub Actions
1. Suba todo o conteúdo desta pasta para um repositório no GitHub (ramo `main`).
2. Se a pasta `.github` não subir, crie `.github/workflows/build.yml` pelo site e cole o conteúdo de `build.yml` (na raiz).
3. Na aba Actions, o fluxo "Build APK" roda sozinho. Baixe `focuslock-apk` em Artifacts.

## Como abrir no Android Studio
Abra a pasta, aguarde a sincronização do Gradle e rode no aparelho. No terminal, com Gradle 8.9: `gradle assembleDebug`.

## Permissões e como funcionam
- **Acessibilidade**: ativada manualmente em Ajustes do sistema. O serviço só vê qual app foi aberto e, no Hardcore, se uma tela de ajustes do sistema trata do FocusLock. Nada é gravado ou enviado.
- **VPN**: o Android pede confirmação. Só as consultas de DNS passam pelo túnel (10.111.0.1); domínios bloqueados recebem NXDOMAIN e os demais seguem para o DNS 8.8.8.8. Não há interceptação de HTTPS.
- **Notificações** (Android 13+): lembretes.
- **Administrador do dispositivo**: opcional; desinstalar o app passa a exigir desativá-lo antes.
- **Internet**: usada apenas pelo encaminhamento de DNS do filtro de sites.

## Limites do Android
- Um APK comum não impede de forma absoluta que o dono do aparelho o desative ou desinstale (modo seguro, ajustes do sistema, reset).
- Recursos como impedir a desinstalação de verdade exigem Device Owner (aparelho configurado como gerenciado), o que não é possível num app instalado normalmente.
- O DNS privado do Android (DNS sobre TLS) e navegadores com DNS próprio podem contornar o filtro de sites. Desative "DNS privado" nos ajustes de rede para o filtro valer.
- O filtro visual local tem a interface e as regras prontas, mas nenhum modelo de imagem acompanha o app.

## Privacidade
Tudo fica no aparelho. Nada é enviado a servidores do FocusLock e imagens nunca são enviadas.
