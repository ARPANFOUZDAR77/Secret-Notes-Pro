package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.SecurityPreferences
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.security.EncryptionManager.init(this)
        enableEdgeToEdge()
        setContent {
            val securityPrefs = remember { SecurityPreferences(this) }
            val appTheme by securityPrefs.appThemeFlow.collectAsState(initial = "Cyber")

            MyApplicationTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(this)
                }
            }
        }
        
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // To implement auto-lock, you'd signal a shared view model here to 
                // navigate back to the auth screen or set a locked flag.
                // For this example, setting a global flag could work.
            }
        })
    }
}
