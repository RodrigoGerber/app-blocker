package com.example.appblocker

import android.app.Application
import com.example.appblocker.di.AppContainer

/**
 * Owns the [AppContainer] so both the UI (via the ViewModel) and the
 * accessibility service can reach the same wired dependencies.
 */
class AppBlockerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
