# App Blocker — Especificação Técnica

## 1. Visão geral

O App Blocker é um aplicativo Android de uso pessoal criado para reduzir o tempo gasto no Instagram.

O aplicativo permitirá definir um limite diário de uso. Quando esse limite for atingido, qualquer nova tentativa de abrir o Instagram fará com que o Android volte automaticamente para a tela inicial.

A primeira versão será desenvolvida exclusivamente para Android, utilizando Kotlin nativo e Jetpack Compose. Não haverá publicação na Google Play Store, suporte a iOS, backend, autenticação ou sincronização entre dispositivos.

---

## 2. Objetivo

O objetivo principal não é impedir tecnicamente qualquer possibilidade de acesso ao Instagram, mas criar atrito suficiente para interromper o comportamento automático de abrir o aplicativo.

O MVP deve:

1. Permitir configurar um limite diário de uso do Instagram.
2. Identificar quando o Instagram está em primeiro plano.
3. Consultar quanto tempo o Instagram foi utilizado no dia.
4. Redirecionar o usuário para a Home quando o limite diário tiver sido atingido.
5. Exibir claramente o estado das permissões necessárias.
6. Continuar funcionando mesmo quando a interface principal do App Blocker estiver fechada.

---

## 3. Escopo do MVP

### 3.1 Funcionalidades incluídas

- Configuração de limite diário em minutos.
- Ativação e desativação do bloqueio.
- Exibição do tempo utilizado no dia.
- Exibição do tempo restante.
- Solicitação e verificação de acesso às estatísticas de uso.
- Solicitação e verificação do serviço de acessibilidade.
- Detecção da abertura do Instagram.
- Redirecionamento para a Home após o limite.
- Persistência das configurações localmente.
- Reinício automático do contador a cada novo dia.
- Interface simples em Jetpack Compose.

### 3.2 Funcionalidades fora do MVP

- Publicação na Google Play Store.
- Suporte a outros aplicativos.
- Regras diferentes por dia da semana.
- Faixas de horário bloqueadas.
- Bloqueio por sessão.
- Overlay sobre o Instagram.
- Senha para desbloqueio.
- Liberação temporária de minutos extras.
- Histórico detalhado por dia.
- Gráficos de uso.
- Conta de usuário.
- Sincronização em nuvem.
- Backend.
- Suporte a múltiplos dispositivos.
- Controle parental ou gerenciamento corporativo.

Esses itens poderão ser adicionados depois que o mecanismo básico estiver funcionando de forma confiável.

---

## 4. Decisões técnicas

### 4.1 Plataforma

- Android nativo.
- Android Studio.
- Kotlin.
- Gradle com Kotlin DSL.

### 4.2 Interface

- Jetpack Compose.
- Material 3.
- Navigation Compose, apenas se mais de uma tela for necessária.
- ViewModel para gerenciamento do estado da interface.
- StateFlow para exposição de estado reativo.

### 4.3 Persistência

- Preferences DataStore no MVP.
- Room não será utilizado inicialmente.

O DataStore armazenará:

- limite diário em minutos;
- bloqueio ativado ou desativado;
- informações auxiliares de configuração;
- última confirmação conhecida das permissões, quando útil.

O tempo de uso não será mantido como fonte principal no DataStore. A fonte principal será o `UsageStatsManager` do Android.

### 4.4 Integrações com o Android

- `AccessibilityService` para detectar a abertura do Instagram.
- `UsageStatsManager` para calcular o tempo de uso no dia.
- `PackageManager` para consultar informações do aplicativo instalado.
- `Settings` intents para abrir as telas de permissões do sistema.
- `performGlobalAction(GLOBAL_ACTION_HOME)` para voltar à Home.

### 4.5 Concorrência

- Kotlin Coroutines.
- CoroutineScope próprio dentro do serviço de acessibilidade.
- Dispatchers apropriados para consultas e persistência.
- Cancelamento do escopo quando o serviço for destruído.

### 4.6 Injeção de dependências

A primeira versão não utilizará Hilt.

Como o projeto será pequeno, as dependências poderão ser construídas manualmente por meio de um container simples da aplicação.

Hilt poderá ser introduzido caso o projeto cresça e passe a ter:

- múltiplas implementações;
- muitos ViewModels;
- banco Room;
- testes mais extensos;
- diversos serviços e repositories.

---

## 5. Arquitetura

O projeto seguirá uma arquitetura em camadas simples, sem aplicar Clean Architecture de forma cerimonial.

```text
Jetpack Compose
      ↓
ViewModel
      ↓
Repositories e serviços de domínio
      ↓
DataStore / UsageStatsManager / APIs do Android
```

O `AccessibilityService` será um ponto de entrada separado:

```text
Instagram aberto
      ↓
AccessibilityService
      ↓
ForegroundAppHandler
      ↓
BlockingPolicy
      ↓
DailyUsageProvider + RuleRepository
      ↓
performGlobalAction(GLOBAL_ACTION_HOME)
```

### 5.1 Princípios

- A interface não acessa diretamente APIs de sistema.
- O serviço de acessibilidade não concentra toda a lógica de negócio.
- A decisão de bloquear será isolada em uma classe própria.
- O cálculo de tempo de uso será isolado e testável.
- O código será otimizado para simplicidade, não para generalidade prematura.
- O Instagram será inicialmente o único aplicativo monitorado.
- O package name poderá ficar centralizado em uma constante.

---

## 6. Estrutura de diretórios

```text
app/
└── src/main/java/com/example/appblocker/
    ├── AppBlockerApplication.kt
    ├── MainActivity.kt
    │
    ├── config/
    │   └── AppBlockerConfig.kt
    │
    ├── di/
    │   └── AppContainer.kt
    │
    ├── ui/
    │   ├── AppBlockerScreen.kt
    │   ├── AppBlockerUiState.kt
    │   ├── AppBlockerViewModel.kt
    │   └── components/
    │       ├── PermissionCard.kt
    │       ├── UsageProgress.kt
    │       └── DailyLimitSelector.kt
    │
    ├── accessibility/
    │   ├── AppBlockerAccessibilityService.kt
    │   ├── ForegroundAppHandler.kt
    │   └── AccessibilityPermissionChecker.kt
    │
    ├── blocking/
    │   ├── BlockingPolicy.kt
    │   ├── BlockingDecision.kt
    │   └── HomeRedirector.kt
    │
    ├── usage/
    │   ├── DailyUsageProvider.kt
    │   ├── AndroidDailyUsageProvider.kt
    │   ├── UsageEventParser.kt
    │   └── UsageAccessPermissionChecker.kt
    │
    ├── rules/
    │   ├── BlockingRule.kt
    │   ├── RuleRepository.kt
    │   └── DataStoreRuleRepository.kt
    │
    └── system/
        ├── SettingsNavigator.kt
        ├── InstalledAppProvider.kt
        └── ClockProvider.kt
```

Essa estrutura poderá ser reduzida durante o desenvolvimento se algumas classes se mostrarem desnecessárias.

---

## 7. Configuração principal

```kotlin
object AppBlockerConfig {
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val DEFAULT_DAILY_LIMIT_MINUTES = 30
}
```

O package name do Instagram será mantido em um único lugar para evitar strings espalhadas pelo projeto.

---

## 8. Modelo de domínio

### 8.1 Regra de bloqueio

```kotlin
data class BlockingRule(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
)
```

Apesar de o MVP monitorar apenas o Instagram, manter o `packageName` na regra facilita uma futura expansão para outros aplicativos.

### 8.2 Uso diário

```kotlin
data class DailyUsage(
    val packageName: String,
    val usedMillis: Long,
) {
    val usedMinutes: Long
        get() = usedMillis / 60_000
}
```

### 8.3 Decisão de bloqueio

```kotlin
sealed interface BlockingDecision {
    data object Allow : BlockingDecision

    data class Block(
        val packageName: String,
        val usedMillis: Long,
        val limitMillis: Long,
    ) : BlockingDecision
}
```

---

## 9. Componentes principais

## 9.1 `AppBlockerAccessibilityService`

Responsável por receber eventos de acessibilidade e detectar quando o Instagram entra em primeiro plano.

Responsabilidades:

- ouvir eventos relevantes;
- extrair o package name;
- ignorar eventos que não pertencem ao Instagram;
- evitar processamento repetido excessivo;
- encaminhar a detecção para o `ForegroundAppHandler`;
- executar a ação de Home quando solicitado;
- manter um escopo de coroutines adequado.

Não deve:

- calcular diretamente todo o histórico de uso;
- acessar diretamente o DataStore;
- conter regras complexas;
- atualizar diretamente a interface Compose.

Exemplo inicial:

```kotlin
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName != AppBlockerConfig.INSTAGRAM_PACKAGE) {
            return
        }

        serviceScope.launch {
            foregroundAppHandler.onAppOpened(packageName)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

A implementação final deverá considerar que alguns aparelhos ou versões do Instagram podem produzir diferentes sequências de eventos.

---

## 9.2 `ForegroundAppHandler`

Orquestra a reação à abertura de um aplicativo monitorado.

```kotlin
class ForegroundAppHandler(
    private val blockingPolicy: BlockingPolicy,
    private val homeRedirector: HomeRedirector,
) {
    suspend fun onAppOpened(packageName: String) {
        when (blockingPolicy.evaluate(packageName)) {
            BlockingDecision.Allow -> Unit
            is BlockingDecision.Block -> homeRedirector.redirectToHome()
        }
    }
}
```

Esse componente evita colocar a regra de decisão dentro do serviço Android.

---

## 9.3 `BlockingPolicy`

Responsável por decidir se o Instagram deve ser bloqueado.

Fluxo:

1. Buscar a regra atual.
2. Verificar se o bloqueio está habilitado.
3. Confirmar que o package corresponde a uma regra.
4. Consultar o tempo de uso no dia.
5. Comparar o uso com o limite.
6. Retornar `Allow` ou `Block`.

```kotlin
class BlockingPolicy(
    private val ruleRepository: RuleRepository,
    private val dailyUsageProvider: DailyUsageProvider,
) {
    suspend fun evaluate(packageName: String): BlockingDecision {
        val rule = ruleRepository.getRule()

        if (!rule.enabled || rule.packageName != packageName) {
            return BlockingDecision.Allow
        }

        val usage = dailyUsageProvider.getUsageToday(packageName)
        val limitMillis = rule.dailyLimitMinutes * 60_000L

        return if (usage.usedMillis >= limitMillis) {
            BlockingDecision.Block(
                packageName = packageName,
                usedMillis = usage.usedMillis,
                limitMillis = limitMillis,
            )
        } else {
            BlockingDecision.Allow
        }
    }
}
```

---

## 9.4 `DailyUsageProvider`

Abstração para consultar o uso diário.

```kotlin
interface DailyUsageProvider {
    suspend fun getUsageToday(packageName: String): DailyUsage
}
```

A implementação Android utilizará `UsageStatsManager`.

```kotlin
class AndroidDailyUsageProvider(
    private val usageStatsManager: UsageStatsManager,
    private val clock: Clock,
    private val eventParser: UsageEventParser,
) : DailyUsageProvider {

    override suspend fun getUsageToday(packageName: String): DailyUsage {
        val now = clock.instant()
        val startOfDay = LocalDate.now(clock)
            .atStartOfDay(clock.zone)
            .toInstant()

        val events = usageStatsManager.queryEvents(
            startOfDay.toEpochMilli(),
            now.toEpochMilli(),
        )

        val usedMillis = eventParser.calculateForegroundTime(
            events = events,
            packageName = packageName,
            intervalEndMillis = now.toEpochMilli(),
        )

        return DailyUsage(
            packageName = packageName,
            usedMillis = usedMillis,
        )
    }
}
```

---

## 9.5 `UsageEventParser`

Responsável por transformar eventos do Android em sessões de uso.

O parser deverá:

- considerar eventos de entrada no foreground;
- considerar eventos de saída do foreground;
- fechar uma sessão ainda aberta usando o instante atual;
- ignorar eventos de outros packages;
- evitar durações negativas;
- tolerar eventos ausentes ou duplicados;
- funcionar corretamente quando não houver uso no dia.

Pseudocódigo:

```text
total = 0
sessionStart = null

para cada evento em ordem:
    se package != Instagram:
        continuar

    se evento indica entrada no foreground:
        se não existe sessão aberta:
            sessionStart = timestamp

    se evento indica saída do foreground:
        se existe sessão aberta:
            total += timestamp - sessionStart
            sessionStart = null

se existe sessão aberta:
    total += agora - sessionStart

retornar total
```

A lista exata de tipos de evento aceitos deverá ser validada durante o spike técnico, considerando a versão mínima do Android escolhida.

---

## 9.6 `HomeRedirector`

Abstração da ação de retornar para a Home.

```kotlin
interface HomeRedirector {
    fun redirectToHome(): Boolean
}
```

Como `performGlobalAction` pertence ao `AccessibilityService`, a implementação poderá ser fornecida pelo próprio serviço ou por um pequeno adaptador.

```kotlin
class AccessibilityHomeRedirector(
    private val service: AccessibilityService,
) : HomeRedirector {

    override fun redirectToHome(): Boolean {
        return service.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_HOME
        )
    }
}
```

Caso a injeção do serviço se torne inconveniente, a primeira implementação poderá executar a ação diretamente no serviço, mantendo a decisão fora dele.

---

## 9.7 `RuleRepository`

```kotlin
interface RuleRepository {
    val rule: Flow<BlockingRule>

    suspend fun getRule(): BlockingRule

    suspend fun setDailyLimitMinutes(minutes: Int)

    suspend fun setEnabled(enabled: Boolean)
}
```

A implementação utilizará Preferences DataStore.

Chaves previstas:

```text
daily_limit_minutes
blocking_enabled
```

Valores padrão:

```text
daily_limit_minutes = 30
blocking_enabled = false
```

---

## 10. Fluxos principais

## 10.1 Configuração inicial

```text
Usuário abre o App Blocker
      ↓
App verifica as permissões
      ↓
Usage Access desativado?
      ├── sim → mostrar ação para abrir configurações
      └── não
      ↓
Accessibility Service desativado?
      ├── sim → mostrar ação para abrir configurações
      └── não
      ↓
Usuário define o limite diário
      ↓
Usuário ativa o bloqueio
      ↓
Configuração salva no DataStore
```

O aplicativo não deve indicar que está totalmente ativo enquanto uma das permissões obrigatórias estiver ausente.

---

## 10.2 Monitoramento

```text
Usuário abre o Instagram
      ↓
AccessibilityService recebe evento
      ↓
Package é com.instagram.android?
      ├── não → ignorar
      └── sim
      ↓
BlockingPolicy consulta a regra
      ↓
Bloqueio está habilitado?
      ├── não → permitir
      └── sim
      ↓
Consultar uso diário
      ↓
Uso >= limite?
      ├── não → permitir
      └── sim → GLOBAL_ACTION_HOME
```

---

## 10.3 Atualização da tela

```text
App volta ao foreground
      ↓
ViewModel verifica permissões
      ↓
ViewModel consulta regra
      ↓
ViewModel consulta uso diário
      ↓
UiState é atualizado
      ↓
Compose renderiza os novos valores
```

Não é necessário atualizar o tempo a cada segundo.

A tela poderá atualizar:

- ao abrir;
- ao voltar das configurações;
- ao mudar o limite;
- ao ativar ou desativar o bloqueio;
- periodicamente enquanto estiver visível, por exemplo a cada 30 segundos.

---

## 11. Estado da interface

```kotlin
data class AppBlockerUiState(
    val isLoading: Boolean = true,
    val dailyLimitMinutes: Int = 30,
    val usedMinutesToday: Long = 0,
    val blockingEnabled: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val remainingMinutes: Long
        get() = (dailyLimitMinutes - usedMinutesToday)
            .coerceAtLeast(0)

    val isReady: Boolean
        get() = hasUsageAccess && hasAccessibilityAccess
}
```

---

## 12. Interface do MVP

A aplicação poderá ter uma única tela.

### Conteúdo sugerido

```text
App Blocker

Instagram
18 de 30 minutos usados hoje

[████████████░░░░░░]

Limite diário
[-] 30 minutos [+]

Bloqueio
[Ativado]

Permissões

✓ Acesso ao uso
✓ Serviço de acessibilidade
```

Quando uma permissão estiver ausente:

```text
! Acesso ao uso necessário
[Conceder acesso]
```

ou:

```text
! Serviço de acessibilidade desativado
[Ativar serviço]
```

### Comportamento do botão de ativação

O bloqueio só poderá ser ativado quando:

- o limite for maior que zero;
- o acesso às estatísticas de uso estiver concedido;
- o serviço de acessibilidade estiver ativado.

Alternativamente, o usuário pode tocar em ativar e o app direcioná-lo à permissão pendente.

---

## 13. Permissões e configuração Android

## 13.1 Usage Access

O `UsageStatsManager` depende de acesso concedido manualmente pelo usuário nas configurações do sistema.

Manifest:

```xml
<uses-permission
    android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

O aplicativo deverá abrir a tela correspondente:

```kotlin
Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
```

A concessão não ocorre por uma caixa de diálogo comum de runtime permission.

---

## 13.2 Accessibility Service

O serviço deve ser declarado no manifest:

```xml
<service
    android:name=".accessibility.AppBlockerAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action
            android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>

    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/app_blocker_accessibility_service" />
</service>
```

Arquivo de configuração:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description" />
```

O conjunto de eventos poderá ser ajustado após testes.

O aplicativo deverá abrir:

```kotlin
Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
```

---

## 14. Performance e controle de eventos

O serviço de acessibilidade pode receber vários eventos em sequência.

Para evitar processamento excessivo:

1. Filtrar imediatamente pelo tipo de evento.
2. Filtrar imediatamente pelo package do Instagram.
3. Aplicar debounce.
4. Impedir avaliações concorrentes.
5. Manter cache curto do uso diário.

Exemplo de comportamento:

```text
Evento 1 do Instagram → processar
Evento 2 após 20 ms   → ignorar
Evento 3 após 80 ms   → ignorar
Evento 4 após 700 ms  → processar se necessário
```

Uma combinação de `Mutex` e timestamp da última avaliação deverá ser suficiente.

```kotlin
private val evaluationMutex = Mutex()
private var lastEvaluationAt = 0L
private val debounceMillis = 500L
```

Também é importante evitar múltiplos comandos de Home consecutivos.

---

## 15. Tratamento de falhas

### 15.1 Acesso de uso revogado

- A tela deve indicar que a permissão foi perdida.
- O bloqueio deve ser considerado inativo ou indisponível.
- O app não deve fingir que está medindo o uso corretamente.

### 15.2 Serviço de acessibilidade desativado

- A tela deve indicar que o monitoramento não está funcionando.
- Deve existir uma ação para abrir as configurações.

### 15.3 Falha ao calcular o uso

A política mais segura para o MVP será permitir o acesso em caso de falha.

```text
Falha ao consultar tempo de uso
      ↓
Não bloquear
      ↓
Registrar erro para diagnóstico
```

O objetivo é evitar um bloqueio incorreto e difícil de entender.

### 15.4 `performGlobalAction` retorna falso

- Registrar a falha.
- Não repetir indefinidamente em loop.
- Permitir nova tentativa em um próximo evento.

### 15.5 Mudança de data

Como o tempo será consultado desde o início do dia atual, não será necessário zerá-lo manualmente.

A implementação deve usar o timezone local do dispositivo.

### 15.6 Reinicialização do aparelho

O serviço de acessibilidade habilitado normalmente permanece configurado, mas o comportamento deve ser validado no aparelho real.

Como a regra está no DataStore e o uso vem do sistema, não deve existir dependência de estado mantido somente em memória.

---

## 16. Logging e diagnóstico

Como o aplicativo será usado apenas pelo desenvolvedor, logs detalhados serão úteis.

Tags sugeridas:

```text
AppBlockerService
BlockingPolicy
DailyUsageProvider
UsageEventParser
PermissionChecker
```

Eventos importantes:

- serviço criado;
- serviço destruído;
- Instagram detectado;
- avaliação iniciada;
- uso calculado;
- regra carregada;
- decisão de permitir;
- decisão de bloquear;
- comando Home executado;
- erro ao consultar eventos;
- permissão ausente.

Não registrar informações sensíveis ou conteúdo de outras janelas.

---

## 17. Testes

## 17.1 Testes unitários

### `BlockingPolicy`

Cenários:

- bloqueio desativado;
- package diferente;
- uso abaixo do limite;
- uso exatamente igual ao limite;
- uso acima do limite;
- limite de zero minutos;
- erro do provider.

### `UsageEventParser`

Cenários:

- nenhuma sessão;
- uma sessão completa;
- múltiplas sessões;
- sessão aberta até o instante atual;
- evento de saída sem entrada;
- entrada duplicada;
- eventos de outros apps;
- timestamps fora de ordem;
- mudança de dia;
- sessão atravessando o início do intervalo.

### `DataStoreRuleRepository`

Cenários:

- valores padrão;
- alteração do limite;
- ativação e desativação;
- persistência após recriação.

### `AppBlockerViewModel`

Cenários:

- permissões concedidas;
- permissão de uso ausente;
- acessibilidade ausente;
- carregamento do uso;
- atualização do limite;
- erro de consulta.

---

## 17.2 Testes manuais

- Abrir o Instagram abaixo do limite.
- Abrir o Instagram exatamente no limite.
- Abrir o Instagram acima do limite.
- Tentar abrir repetidamente após o bloqueio.
- Abrir o Instagram por uma notificação.
- Abrir o Instagram por um link externo.
- Abrir o Instagram pela tela de aplicativos recentes.
- Usar picture-in-picture, caso aplicável.
- Bloquear e desbloquear a tela.
- Reiniciar o aparelho.
- Revogar Usage Access.
- Desativar Accessibility Service.
- Alterar o horário do aparelho.
- Virar a data à meia-noite.
- Alterar o limite durante o dia.
- Desativar o bloqueio após atingir o limite.
- Forçar parada do App Blocker.
- Verificar otimizações de bateria do fabricante.

---

## 18. Spike técnico inicial

Antes de desenvolver a interface completa, será criado um protótipo mínimo.

### Objetivos do spike

1. Declarar e ativar o `AccessibilityService`.
2. Detectar eventos do Instagram.
3. Registrar o package name no Logcat.
4. Executar `GLOBAL_ACTION_HOME`.
5. Solicitar Usage Access.
6. Consultar os eventos de uso do dia.
7. Calcular aproximadamente o tempo total do Instagram.
8. Testar o comportamento no aparelho real.

### Critério de sucesso

O spike será considerado bem-sucedido quando:

- o serviço detectar de forma consistente a abertura do Instagram;
- o redirecionamento para a Home funcionar;
- o tempo diário puder ser consultado com precisão aceitável;
- o mecanismo continuar funcionando com a Activity principal fechada.

Somente depois disso será construída a interface final.

---

## 19. Etapas de implementação

## Fase 1 — Projeto base

- Criar projeto Kotlin com Compose.
- Configurar Material 3.
- Criar `AppBlockerApplication`.
- Criar DataStore.
- Criar tela inicial estática.
- Definir versão mínima do Android.

## Fase 2 — Permissões

- Implementar verificação de Usage Access.
- Implementar navegação para Usage Access Settings.
- Declarar o Accessibility Service.
- Implementar verificação do serviço.
- Implementar navegação para Accessibility Settings.

## Fase 3 — Spike de detecção

- Receber eventos de mudança de janela.
- Filtrar pelo Instagram.
- Registrar eventos.
- Redirecionar para Home sem considerar limite.

Essa etapa deve ser protegida por uma flag de debug para evitar bloqueio acidental durante o desenvolvimento.

## Fase 4 — Persistência da regra

- Criar `BlockingRule`.
- Criar `RuleRepository`.
- Persistir limite diário.
- Persistir estado ativado/desativado.
- Integrar com ViewModel.

## Fase 5 — Cálculo de uso

- Implementar `DailyUsageProvider`.
- Implementar parser de eventos.
- Exibir uso na tela.
- Adicionar testes unitários.

## Fase 6 — Política de bloqueio

- Criar `BlockingPolicy`.
- Integrar com o serviço.
- Aplicar debounce.
- Evitar avaliações concorrentes.
- Executar Home apenas quando necessário.

## Fase 7 — Estabilização

- Testar reinicialização.
- Testar virada de data.
- Testar links e notificações.
- Tratar permissões revogadas.
- Melhorar logs.
- Ajustar comportamento no aparelho real.

---

## 20. Critérios de conclusão do MVP

O MVP estará concluído quando todos os itens abaixo forem verdadeiros:

- O usuário consegue definir um limite diário.
- O limite permanece salvo após fechar o aplicativo.
- O usuário consegue ativar e desativar o bloqueio.
- O app identifica corretamente as permissões ausentes.
- O app abre as telas de configuração necessárias.
- A tela mostra o tempo aproximado usado no dia.
- O serviço detecta a abertura do Instagram.
- O Instagram funciona normalmente antes do limite.
- Após o limite, a abertura do Instagram leva o usuário à Home.
- O comportamento funciona com a Activity principal fechada.
- A mudança de dia começa uma nova contagem.
- O aplicativo não entra em loop de comandos de Home.
- Falhas de consulta não causam bloqueios indevidos.

---

## 21. Melhorias futuras

Após validar o MVP, poderão ser consideradas:

### 21.1 Liberação temporária

- Liberar por cinco minutos.
- Exigir pressionamento longo.
- Aplicar cooldown entre liberações.
- Registrar quantas liberações foram solicitadas.

### 21.2 Overlay

Substituir ou complementar o redirecionamento para Home com um overlay explicativo.

Exemplo:

```text
Você atingiu seu limite diário de Instagram.

30 de 30 minutos utilizados.

[Voltar para a Home]
```

### 21.3 Outros aplicativos

- Seleção de apps instalados.
- Uma regra por aplicativo.
- Limite compartilhado por grupo.
- Redes sociais como categoria.

### 21.4 Regras por horário

- Bloquear durante o trabalho.
- Bloquear na primeira hora do dia.
- Bloquear antes de dormir.
- Permitir dias específicos.

### 21.5 Histórico

- Uso por dia.
- Média semanal.
- Número de tentativas após o bloqueio.
- Comparação com semanas anteriores.

Nesse momento, Room poderá substituir ou complementar o DataStore.

---

## 22. Riscos conhecidos

### Variações entre fabricantes

Samsung, Xiaomi, Motorola e outros fabricantes podem alterar:

- gerenciamento de bateria;
- reinicialização de serviços;
- telas de configuração;
- comportamento de processos em background.

Como o aplicativo será de uso pessoal, a prioridade será funcionar corretamente no aparelho alvo.

### Eventos de acessibilidade inconsistentes

O Instagram pode produzir eventos diferentes conforme:

- versão do aplicativo;
- Activity aberta;
- abertura por deep link;
- compartilhamento;
- notificações;
- mudanças futuras no aplicativo.

O filtro deverá ser testado empiricamente.

### Precisão do UsageStatsManager

O histórico de eventos pode conter casos incompletos ou inesperados.

O objetivo não exige precisão financeira ou científica. Pequenas diferenças são aceitáveis, desde que o bloqueio seja consistente o bastante para reduzir o uso.

### Facilidade de desativação

O próprio usuário sempre poderá:

- desativar o serviço;
- revogar a permissão;
- desligar o bloqueio;
- desinstalar o aplicativo.

Isso é aceitável. O produto é uma ferramenta de autocontrole, não um mecanismo de segurança contra um usuário adversarial.

---

## 23. Definição final da primeira versão

A primeira versão terá:

```text
Plataforma: Android
Linguagem: Kotlin
UI: Jetpack Compose
Persistência: DataStore
Detecção: AccessibilityService
Medição: UsageStatsManager
Ação de bloqueio: GLOBAL_ACTION_HOME
App monitorado: Instagram
Distribuição: instalação local
Backend: nenhum
Conta: nenhuma
```

A arquitetura será mantida pequena e orientada ao objetivo central: detectar a abertura do Instagram, verificar o limite diário e redirecionar o usuário para a Home quando necessário.
