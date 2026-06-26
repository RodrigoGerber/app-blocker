package com.example.appblocker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appblocker.ui.theme.AppBlockerTheme

/**
 * Shows "<used> of <limit> minutes used today" with a progress bar.
 */
@Composable
fun UsageProgress(
    appName: String,
    usedMinutes: Long,
    limitMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (limitMinutes > 0) {
        (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = appName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "$usedMinutes of $limitMinutes minutes used today",
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageProgressPreview() {
    AppBlockerTheme {
        UsageProgress(appName = "Instagram", usedMinutes = 18, limitMinutes = 30)
    }
}
