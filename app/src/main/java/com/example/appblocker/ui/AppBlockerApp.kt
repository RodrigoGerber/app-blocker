package com.example.appblocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appblocker.ui.components.RuleEditorSheet

private object Routes {
    const val RULES = "rules"
    const val PICKER = "picker"
}

/**
 * App root: hosts the rules list and the app picker, and renders the add/edit
 * bottom sheet on top of whichever screen is showing when an editor is active.
 */
@Composable
fun AppBlockerApp(viewModel: AppBlockerViewModel) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.RULES) {
        composable(Routes.RULES) {
            RulesListScreen(
                state = state,
                onAddClick = {
                    viewModel.loadPicker()
                    navController.navigate(Routes.PICKER)
                },
                onRuleClick = viewModel::beginEditRule,
                onRuleToggle = viewModel::setRuleEnabled,
                onPausedChange = viewModel::setPaused,
                onGrantUsageAccess = viewModel::openUsageAccessSettings,
                onEnableAccessibility = viewModel::openAccessibilitySettings,
            )
        }

        composable(Routes.PICKER) {
            val pickerState by viewModel.pickerState.collectAsStateWithLifecycle()
            AppPickerScreen(
                state = pickerState,
                onQueryChange = viewModel::setPickerQuery,
                onSelect = { app ->
                    viewModel.beginAddRule(app)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }

    state.editor?.let { editor ->
        RuleEditorSheet(
            editor = editor,
            onLimitChange = viewModel::updateEditorLimit,
            onEnabledChange = viewModel::updateEditorEnabled,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::deleteEditingRule,
            onDismiss = viewModel::dismissEditor,
        )
    }
}
