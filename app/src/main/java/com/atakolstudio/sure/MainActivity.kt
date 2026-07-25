package com.atakolstudio.sure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.atakolstudio.sure.ui.navigation.SureNavGraph
import com.atakolstudio.sure.ui.theme.SureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ Splash Screen API — super.onCreate()'ten ÖNCE çağrılmalı.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SureApp()
        }
    }
}

@Composable
fun SureApp() {
    SureTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SureNavGraph()
        }
    }
}
