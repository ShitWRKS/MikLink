package com.app.miklink

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.app.miklink.ui.NavGraph
import com.app.miklink.ui.testing.AgentSemanticsConfig
import com.app.miklink.ui.theme.MikLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            MikLinkTheme(
                darkTheme = isDarkTheme
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(AgentSemanticsConfig.rootModifier())
                ) {
                    NavGraph()
                }
            }
        }
    }
}
