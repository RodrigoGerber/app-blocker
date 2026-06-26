package com.example.appblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.appblocker.ui.AppBlockerApp
import com.example.appblocker.ui.AppBlockerViewModel
import com.example.appblocker.ui.theme.AppBlockerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AppBlockerViewModel by viewModels {
        AppBlockerViewModel.Factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppBlockerTheme {
                AppBlockerApp(viewModel = viewModel)
            }
        }

        // Refresh on every foreground entry (covers returning from Settings) and
        // then lightly poll while visible — no need for per-second updates
        // (spec §10.3).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.refresh()
                    delay(REFRESH_INTERVAL_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 30_000L
    }
}
