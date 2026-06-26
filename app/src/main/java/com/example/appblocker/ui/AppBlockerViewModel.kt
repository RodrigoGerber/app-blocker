package com.example.appblocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.appblocker.AppBlockerApplication
import com.example.appblocker.accessibility.AccessibilityPermissionChecker
import com.example.appblocker.config.AppBlockerConfig
import com.example.appblocker.rules.BlockingRule
import com.example.appblocker.rules.RuleRepository
import com.example.appblocker.system.InstalledApp
import com.example.appblocker.system.InstalledAppProvider
import com.example.appblocker.system.SettingsNavigator
import com.example.appblocker.usage.DailyUsageProvider
import com.example.appblocker.usage.UsageAccessPermissionChecker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds state for the rules list, the app picker, and the add/edit editor, and
 * mediates between the UI and the domain layer (spec §5.1, §10.3). The UI never
 * touches system APIs directly.
 *
 * State is refreshed on demand ([refresh]); the Activity calls it on resume,
 * after returning from Settings, and on a light periodic timer while visible.
 */
class AppBlockerViewModel(
    private val ruleRepository: RuleRepository,
    private val dailyUsageProvider: DailyUsageProvider,
    private val usageAccessChecker: UsageAccessPermissionChecker,
    private val accessibilityChecker: AccessibilityPermissionChecker,
    private val settingsNavigator: SettingsNavigator,
    private val installedAppProvider: InstalledAppProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppBlockerUiState())
    val uiState: StateFlow<AppBlockerUiState> = _uiState.asStateFlow()

    private val _pickerState = MutableStateFlow(PickerState())
    val pickerState: StateFlow<PickerState> = _pickerState.asStateFlow()

    // Label+icon cache so icons don't re-rasterize (and flicker) on each refresh.
    private val appInfoCache = ConcurrentHashMap<String, InstalledApp>()

    private var allPickerApps: List<InstalledApp> = emptyList()

    init {
        refresh()
    }

    // --- Rules list -------------------------------------------------------

    /** Recompute permissions, pause state, rules and per-rule usage. */
    fun refresh() {
        viewModelScope.launch {
            val hasUsageAccess = usageAccessChecker.hasUsageAccess()
            val hasAccessibility = accessibilityChecker.isServiceEnabled()
            val paused = ruleRepository.isPaused()
            val rules = ruleRepository.getRules()

            val rows = coroutineScope {
                rules.map { rule ->
                    async {
                        val info = appInfo(rule.packageName)
                        // Usage only with the access grant; else show 0 and never
                        // pretend we're measuring correctly (spec §15.1).
                        val used = if (hasUsageAccess) {
                            runCatching {
                                dailyUsageProvider.getUsageToday(rule.packageName).usedMinutes
                            }.getOrElse { 0L }
                        } else {
                            0L
                        }
                        RuleRow(
                            packageName = rule.packageName,
                            label = info?.label ?: rule.packageName,
                            icon = info?.icon,
                            limitMinutes = rule.dailyLimitMinutes,
                            usedMinutesToday = used,
                            enabled = rule.enabled,
                        )
                    }
                }.awaitAll()
            }.sortedBy { it.label.lowercase() }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    paused = paused,
                    rules = rows,
                    hasUsageAccess = hasUsageAccess,
                    hasAccessibilityAccess = hasAccessibility,
                )
            }
        }
    }

    fun setPaused(paused: Boolean) {
        viewModelScope.launch {
            ruleRepository.setPaused(paused)
            refresh()
        }
    }

    fun setRuleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            // Only allow enabling when prerequisites are met (spec §12).
            if (enabled && !_uiState.value.hasAllPermissions) {
                refresh()
                return@launch
            }
            ruleRepository.setEnabled(packageName, enabled)
            refresh()
        }
    }

    // --- Editor (add / edit) ---------------------------------------------

    fun beginEditRule(row: RuleRow) {
        _uiState.update {
            it.copy(
                editor = EditorState(
                    packageName = row.packageName,
                    label = row.label,
                    icon = row.icon,
                    limitMinutes = row.limitMinutes,
                    enabled = row.enabled,
                    isNew = false,
                ),
            )
        }
    }

    fun beginAddRule(app: InstalledApp) {
        appInfoCache[app.packageName] = app
        _uiState.update {
            it.copy(
                editor = EditorState(
                    packageName = app.packageName,
                    label = app.label,
                    icon = app.icon,
                    limitMinutes = AppBlockerConfig.DEFAULT_DAILY_LIMIT_MINUTES,
                    enabled = true,
                    isNew = true,
                ),
            )
        }
    }

    fun updateEditorLimit(minutes: Int) {
        _uiState.update { it.copy(editor = it.editor?.copy(limitMinutes = minutes.coerceAtLeast(0))) }
    }

    fun updateEditorEnabled(enabled: Boolean) {
        _uiState.update { it.copy(editor = it.editor?.copy(enabled = enabled)) }
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            ruleRepository.upsertRule(
                BlockingRule(
                    packageName = editor.packageName,
                    dailyLimitMinutes = editor.limitMinutes,
                    enabled = editor.enabled,
                ),
            )
            _uiState.update { it.copy(editor = null) }
            refresh()
        }
    }

    fun deleteEditingRule() {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            ruleRepository.deleteRule(editor.packageName)
            _uiState.update { it.copy(editor = null) }
            refresh()
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    // --- App picker -------------------------------------------------------

    fun loadPicker() {
        _pickerState.update { it.copy(isLoading = true, query = "") }
        viewModelScope.launch {
            val existing = ruleRepository.getRules().map { it.packageName }.toSet()
            val apps = installedAppProvider.getLaunchableApps()
                .filterNot { it.packageName in existing }
            apps.forEach { appInfoCache.putIfAbsent(it.packageName, it) }
            allPickerApps = apps
            _pickerState.value = PickerState(isLoading = false, apps = apps, query = "")
        }
    }

    fun setPickerQuery(query: String) {
        _pickerState.update { it.copy(query = query, apps = filterApps(allPickerApps, query)) }
    }

    // --- Settings ---------------------------------------------------------

    fun openUsageAccessSettings() = settingsNavigator.openUsageAccessSettings()

    fun openAccessibilitySettings() = settingsNavigator.openAccessibilitySettings()

    // --- Internals --------------------------------------------------------

    private suspend fun appInfo(packageName: String): InstalledApp? {
        appInfoCache[packageName]?.let { return it }
        return installedAppProvider.getInstalledApp(packageName)?.also {
            appInfoCache[packageName] = it
        }
    }

    private fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val q = query.trim()
        if (q.isEmpty()) return apps
        return apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as AppBlockerApplication
                val container = app.container
                AppBlockerViewModel(
                    ruleRepository = container.ruleRepository,
                    dailyUsageProvider = container.dailyUsageProvider,
                    usageAccessChecker = container.usageAccessChecker,
                    accessibilityChecker = container.accessibilityChecker,
                    settingsNavigator = container.settingsNavigator,
                    installedAppProvider = container.installedAppProvider,
                )
            }
        }
    }
}
