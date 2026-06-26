package com.example.appblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appblocker.ui.theme.AppBlockerTheme

/**
 * Stepper for the daily limit in minutes. Steps by [step] and clamps to
 * [minMinutes]..[maxMinutes]. Uses text buttons to avoid pulling in the
 * material-icons dependency for two glyphs.
 */
@Composable
fun DailyLimitSelector(
    limitMinutes: Int,
    onLimitChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 5,
    minMinutes: Int = 0,
    maxMinutes: Int = 600,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Daily limit",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = {
                    onLimitChange((limitMinutes - step).coerceAtLeast(minMinutes))
                },
                enabled = limitMinutes > minMinutes,
                modifier = Modifier.size(56.dp),
            ) {
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "$limitMinutes minutes",
                style = MaterialTheme.typography.headlineSmall,
            )

            FilledTonalButton(
                onClick = {
                    onLimitChange((limitMinutes + step).coerceAtMost(maxMinutes))
                },
                enabled = limitMinutes < maxMinutes,
                modifier = Modifier.size(56.dp),
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyLimitSelectorPreview() {
    AppBlockerTheme {
        DailyLimitSelector(limitMinutes = 30, onLimitChange = {})
    }
}
