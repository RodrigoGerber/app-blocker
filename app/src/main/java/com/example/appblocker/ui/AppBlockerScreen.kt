package com.example.appblocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appblocker.ui.components.DailyLimitSelector
import com.example.appblocker.ui.components.PermissionCard
import com.example.appblocker.ui.components.UsageProgress
import com.example.appblocker.ui.theme.AppBlockerTheme

/**
 * Single-screen UI for the MVP (spec §12). Stateless: it renders [state] and
 * forwards user intents through the callbacks, so it is easy to preview.
 */
@Composable
fun AppBlockerScreen(
    state: AppBlockerUiState,
    onLimitChange: (Int) -> Unit,
    onBlockingToggle: (Boolean) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onEnableAccessibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "App Blocker",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(24.dp))

        UsageProgress(
            appName = "Instagram",
            usedMinutes = state.usedMinutesToday,
            limitMinutes = state.dailyLimitMinutes,
        )
        Text(
            text = "${state.remainingMinutes} minutes remaining",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(24.dp))

        DailyLimitSelector(
            limitMinutes = state.dailyLimitMinutes,
            onLimitChange = onLimitChange,
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = "Blocking", style = MaterialTheme.typography.titleMedium)
                if (!state.canEnableBlocking && !state.blockingEnabled) {
                    Text(
                        text = "Grant both permissions and set a limit first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = state.blockingEnabled,
                onCheckedChange = onBlockingToggle,
                // Allow turning OFF anytime; only gate turning ON.
                enabled = state.blockingEnabled || state.canEnableBlocking,
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(text = "Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        PermissionCard(
            label = "Usage access",
            granted = state.hasUsageAccess,
            missingMessage = "Usage access required",
            actionLabel = "Grant access",
            onActionClick = onGrantUsageAccess,
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            label = "Accessibility service",
            granted = state.hasAccessibilityAccess,
            missingMessage = "Accessibility service disabled",
            actionLabel = "Enable service",
            onActionClick = onEnableAccessibility,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBlockerScreenReadyPreview() {
    AppBlockerTheme {
        AppBlockerScreen(
            state = AppBlockerUiState(
                isLoading = false,
                dailyLimitMinutes = 30,
                usedMinutesToday = 18,
                blockingEnabled = true,
                hasUsageAccess = true,
                hasAccessibilityAccess = true,
            ),
            onLimitChange = {},
            onBlockingToggle = {},
            onGrantUsageAccess = {},
            onEnableAccessibility = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBlockerScreenMissingPermissionsPreview() {
    AppBlockerTheme {
        AppBlockerScreen(
            state = AppBlockerUiState(
                isLoading = false,
                dailyLimitMinutes = 30,
                usedMinutesToday = 0,
                blockingEnabled = false,
                hasUsageAccess = false,
                hasAccessibilityAccess = false,
            ),
            onLimitChange = {},
            onBlockingToggle = {},
            onGrantUsageAccess = {},
            onEnableAccessibility = {},
        )
    }
}
