package com.example.appblocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appblocker.ui.components.PermissionCard
import com.example.appblocker.ui.components.RuleRowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    state: AppBlockerUiState,
    onAddClick: () -> Unit,
    onRuleClick: (RuleRow) -> Unit,
    onRuleToggle: (String, Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onEnableAccessibility: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("App Blocker") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(innerPadding)
            else -> RulesContent(
                state = state,
                innerPadding = innerPadding,
                onRuleClick = onRuleClick,
                onRuleToggle = onRuleToggle,
                onPausedChange = onPausedChange,
                onGrantUsageAccess = onGrantUsageAccess,
                onEnableAccessibility = onEnableAccessibility,
            )
        }
    }
}

@Composable
private fun LoadingState(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RulesContent(
    state: AppBlockerUiState,
    innerPadding: PaddingValues,
    onRuleClick: (RuleRow) -> Unit,
    onRuleToggle: (String, Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onEnableAccessibility: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.hasUsageAccess) {
            item {
                PermissionCard(
                    message = "Usage access required",
                    actionLabel = "Grant access",
                    onActionClick = onGrantUsageAccess,
                )
            }
        }
        if (!state.hasAccessibilityAccess) {
            item {
                PermissionCard(
                    message = "Accessibility service disabled",
                    actionLabel = "Enable service",
                    onActionClick = onEnableAccessibility,
                )
            }
        }

        item {
            PauseRow(paused = state.paused, onPausedChange = onPausedChange)
        }

        if (state.rules.isEmpty()) {
            item { EmptyState() }
        } else {
            items(state.rules, key = { it.packageName }) { row ->
                RuleRowCard(
                    row = row,
                    onClick = { onRuleClick(row) },
                    onToggle = { enabled -> onRuleToggle(row.packageName, enabled) },
                )
            }
        }
    }
}

@Composable
private fun PauseRow(paused: Boolean, onPausedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Pause all blocking", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (paused) "All rules suspended" else "Rules active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = paused, onCheckedChange = onPausedChange)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No apps blocked yet.\nTap + to add one.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
