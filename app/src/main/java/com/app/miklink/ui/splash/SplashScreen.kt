package com.app.miklink.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.miklink.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(key1 = true) {
        // Template: "Premium Entry" (Zoom In + Fade Out to reveal app)
        
        // Step 1: Init - Start slightly smaller
        scale.snapTo(0.8f)
        alpha.snapTo(0f)

        // Step 2: Fade In + Scale to Normal
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        
        // Wait for logo to be fully visible and read
        delay(1200)

        // Step 3: Exit Animation - Zoom In massively (to "enter" the app) + Fade Out
        launch {
            scale.animateTo(
                targetValue = 5.0f,
                animationSpec = tween(durationMillis = 600, easing = androidx.compose.animation.core.EaseInExpo)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400) // Fade out faster
            )
        }

        // Wait for exit to finish
        delay(500)

        // Navigate to Dashboard
        navController.navigate("dashboard") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}
