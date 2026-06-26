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
import com.example.appblocker.system.SettingsNavigator
import com.example.appblocker.usage.DailyUsageProvider
import com.example.appblocker.usage.UsageAccessPermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the screen state and mediates between the UI and the domain layer. The
 * UI never touches system APIs directly (spec §5.1, §10.3).
 *
 * State is refreshed on demand ([refresh]) rather than ticking every second:
 * the Activity calls it on resume, after returning from Settings, and on a
 * light periodic timer while visible.
 *
 * NOTE: this is a temporary single-app bridge over the new list-based
 * [RuleRepository], pinned to the Instagram rule. It is replaced by the
 * multi-rule UI in step 4 of the multi-app plan.
 */
class AppBlockerViewModel(
    private val ruleRepository: RuleRepository,
    private val dailyUsageProvider: DailyUsageProvider,
    private val usageAccessChecker: UsageAccessPermissionChecker,
    private val accessibilityChecker: AccessibilityPermissionChecker,
    private val settingsNavigator: SettingsNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppBlockerUiState())
    val uiState: StateFlow<AppBlockerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Recompute permissions, rule and usage. Safe to call repeatedly. */
    fun refresh() {
        viewModelScope.launch {
            val hasUsageAccess = usageAccessChecker.hasUsageAccess()
            val hasAccessibility = accessibilityChecker.isServiceEnabled()
            val rule = ruleRepository.getRule(BRIDGED_PACKAGE)

            val limitMinutes = rule?.dailyLimitMinutes ?: AppBlockerConfig.DEFAULT_DAILY_LIMIT_MINUTES
            val enabled = rule?.enabled ?: false

            // Usage can only be read with the access grant; otherwise show 0 and
            // never pretend we are measuring correctly (spec §15.1).
            val usedMinutes = if (hasUsageAccess) {
                runCatching {
                    dailyUsageProvider.getUsageToday(BRIDGED_PACKAGE).usedMinutes
                }.getOrElse { 0L }
            } else {
                0L
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    dailyLimitMinutes = limitMinutes,
                    usedMinutesToday = usedMinutes,
                    blockingEnabled = enabled,
                    hasUsageAccess = hasUsageAccess,
                    hasAccessibilityAccess = hasAccessibility,
                )
            }
        }
    }

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch {
            val current = ruleRepository.getRule(BRIDGED_PACKAGE)
            ruleRepository.upsertRule(
                BlockingRule(
                    packageName = BRIDGED_PACKAGE,
                    dailyLimitMinutes = minutes.coerceAtLeast(0),
                    enabled = current?.enabled ?: false,
                ),
            )
            refresh()
        }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Guard: only allow enabling when prerequisites are met (spec §12).
            if (enabled && !_uiState.value.canEnableBlocking) {
                refresh()
                return@launch
            }
            val current = ruleRepository.getRule(BRIDGED_PACKAGE)
            ruleRepository.upsertRule(
                BlockingRule(
                    packageName = BRIDGED_PACKAGE,
                    dailyLimitMinutes = current?.dailyLimitMinutes
                        ?: _uiState.value.dailyLimitMinutes,
                    enabled = enabled,
                ),
            )
            refresh()
        }
    }

    fun openUsageAccessSettings() = settingsNavigator.openUsageAccessSettings()

    fun openAccessibilitySettings() = settingsNavigator.openAccessibilitySettings()

    companion object {
        // The single-app bridge operates on the Instagram rule until step 4.
        private const val BRIDGED_PACKAGE = AppBlockerConfig.INSTAGRAM_PACKAGE

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
                )
            }
        }
    }
}
