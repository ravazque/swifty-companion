package com.ravazque.swiftycompanion

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ravazque.swiftycompanion.data.auth.REDIRECT_URI
import com.ravazque.swiftycompanion.ui.AppNavHost
import com.ravazque.swiftycompanion.ui.components.AppLanguage
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SwiftyTheme {
                AppNavHost()
            }
        }
        // A recreated activity (rotation) still holds the old redirect: only a fresh launch reads it here.
        if (savedInstanceState == null) handleSignInRedirect(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSignInRedirect(intent)
    }

    private fun handleSignInRedirect(intent: Intent?) {
        val uri = intent?.dataString ?: return
        if (uri.startsWith(REDIRECT_URI)) (application as SwiftyApp).container.session.onRedirect(uri)
    }
}
