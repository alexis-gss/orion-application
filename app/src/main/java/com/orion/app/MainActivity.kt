package com.orion.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.orion.app.core.data.ThemePreferenceStore
import com.orion.app.core.ui.RootApp
import com.orion.app.core.ui.theme.OrionTheme
import com.orion.app.core.ui.theme.ThemeUniverse

/** Single Activity hosting the whole Compose UI tree (see [RootApp] for navigation). */
class MainActivity : AppCompatActivity() {
    /** Reads the persisted theme preference, then sets the Compose content for the whole app. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OrionApplication
        setContent {
            val themeStore = remember { ThemePreferenceStore(applicationContext, lifecycleScope) }
            val isDarkTheme by themeStore.isDarkTheme.collectAsState()
            // Updated by RootApp based on the active route: yellow gradient for cinema,
            // Twitch-style purple gradient for video games.
            val themeUniverseState = remember { mutableStateOf(ThemeUniverse.CINEMA) }

            OrionTheme(darkTheme = isDarkTheme, universe = themeUniverseState.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootApp(app = app, themeStore = themeStore, themeUniverseState = themeUniverseState)
                }
            }
        }
    }
}
