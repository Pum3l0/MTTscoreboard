package io.github.pum3l0.scoreboard

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.pum3l0.scoreboard.data.settings.AppLanguage
import io.github.pum3l0.scoreboard.data.settings.SETTINGS_FILE
import io.github.pum3l0.scoreboard.ui.ScoreBoardApp
import io.github.pum3l0.scoreboard.ui.match.MatchViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        val preferences = base.getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE)
        val language = AppLanguage.stored(preferences)
            ?: AppLanguage.forLocale(base.resources.configuration.locales[0])
        val override = Configuration().apply { setLocale(language.locale) }
        super.attachBaseContext(base.createConfigurationContext(override))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ScoreBoardApplication
        val factory = viewModelFactory { initializer { MatchViewModel(app.matches) } }
        val viewModel = ViewModelProvider(this, factory)[MatchViewModel::class.java]
        val systemBars = WindowCompat.getInsetsController(window, window.decorView)
        setContent {
            ScoreBoardApp(
                viewModel = viewModel,
                theme = app.theme,
                onLanguageSelected = { language ->
                    val current = app.theme.settings.value
                    if (current.language != language) {
                        app.theme.save(current.copy(language = language))
                        if (resources.configuration.locales[0] != language.locale) recreate()
                    }
                },
            ) { dark ->
                systemBars.isAppearanceLightStatusBars = !dark
                systemBars.isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
