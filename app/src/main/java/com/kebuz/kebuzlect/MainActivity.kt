package com.kebuz.kebuzlect

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kebuz.kebuzlect.data.settings.AppSettings
import com.kebuz.kebuzlect.ui.navigation.KebuzNavHost
import com.kebuz.kebuzlect.ui.theme.KebuzLectMobileTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val bootDark = when (AppSettings.bootTheme(this)) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
        setTheme(if (bootDark) R.style.Theme_KebuzLectMobile_Dark else R.style.Theme_KebuzLectMobile_Light)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = remember { AppSettings(applicationContext) }
            val state by settings.state.collectAsStateWithLifecycle(initialValue = null)
            val loaded = state ?: return@setContent
            LaunchedEffect(loaded.theme) {
                AppSettings.writeBootTheme(applicationContext, loaded.theme)
            }
            val darkTheme = when (loaded.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            LocalizedContent(language = loaded.language) {
                KebuzLectMobileTheme(darkTheme = darkTheme) {
                    KebuzNavHost()
                }
            }
        }
    }
}

@Composable
private fun LocalizedContent(language: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val localizedContext = remember(language, context) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        context.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner!!,
        content = content,
    )
}
