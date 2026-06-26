package com.example.appblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appblocker.ui.theme.AppBlockerTheme

/**
 * One row describing the state of a required permission. When [granted] is
 * false, an action button is shown to open the relevant Settings screen
 * (spec §12, §15).
 */
@Composable
fun PermissionCard(
    label: String,
    granted: Boolean,
    missingMessage: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = if (granted) "✓" else "!")
                Text(
                    text = if (granted) label else missingMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (!granted) {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardPreview() {
    AppBlockerTheme {
        Column {
            PermissionCard(
                label = "Usage access",
                granted = true,
                missingMessage = "Usage access required",
                actionLabel = "Grant access",
                onActionClick = {},
            )
            PermissionCard(
                label = "Accessibility service",
                granted = false,
                missingMessage = "Accessibility service disabled",
                actionLabel = "Enable service",
                onActionClick = {},
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
