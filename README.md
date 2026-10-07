# FocusLock

"Recupere o controle do seu tempo." App Android de autocontrole e disciplina digital.

## Etapa 1 (esta versão)
- Kotlin, Jetpack Compose, Material 3, Room, MVVM, Navigation Compose
- Dashboard com sequência, melhor sequência, XP, nível e progresso do desafio
- Calendário de disciplina (15 semanas) com detalhe do dia ao tocar
- Desafios (nome e duração), objetivos com XP, níveis e testes unitários
- Tema claro e escuro automático
- Todos os dados ficam só no aparelho; o app não usa internet nem pede permissões

## Próximas etapas
1. Bloqueio de aplicativos (AccessibilityService) e tela de bloqueio
2. Filtro de sites por DNS (VpnService local)
3. Arquitetura do filtro visual local (TensorFlow Lite / LiteRT)
4. Modo Hardcore (DevicePolicyManager) e notificações
5. Onboarding e demais entidades (BlockedApp, BlockedDomain, Achievement, AppSettings)

## Como gerar o APK com GitHub Actions
1. Suba todo o conteúdo desta pasta para um repositório no GitHub (ramo `main`).
2. Se a pasta `.github` não subir, crie o arquivo `.github/workflows/build.yml` pelo site e cole o conteúdo de `build.yml` (na raiz).
3. Abra a aba Actions. O fluxo "Build APK" roda sozinho (ou use "Run workflow").
4. Ao terminar, baixe `focuslock-apk` em Artifacts. O APK está dentro do ZIP.

## Como abrir no Android Studio
Abra a pasta do projeto, aguarde a sincronização do Gradle e rode no aparelho ou emulador.
Para gerar pelo terminal, com Gradle 8.9 instalado: `gradle assembleDebug`.

## Limites do Android (importante)
- Um APK comum não consegue impedir de forma absoluta a desativação ou a desinstalação pelo dono do aparelho.
- O bloqueio de apps depende do Serviço de Acessibilidade, ativado manualmente pelo usuário.
- A VPN local só filtra por domínio (DNS). Não intercepta nem quebra HTTPS.
- Recursos de Device Owner exigem configuração externa do aparelho e serão documentados na etapa do modo Hardcore.
