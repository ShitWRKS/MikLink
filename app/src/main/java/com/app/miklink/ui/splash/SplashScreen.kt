/*
 * Purpose: Display animated splash with brand assets and transition to dashboard after intro animations.
 * Inputs: NavController for navigation; relies on Coil image loader for GIF rendering.
 * Outputs: Navigates to dashboard after animation and shows branding text/assets with theme colors.
 */
package com.app.miklink.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.app.miklink.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.app.miklink.ui.theme.JetBrainsMono
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) } // Progress state
    val context = androidx.compose.ui.platform.LocalContext.current

    // Coil ImageLoader for GIFs
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
            }
            .build()
    }

    LaunchedEffect(key1 = true) {
        // Animation Sequence
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        
        // Progress Animation (Simulate loading)
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
            )
        }
        
        // Hold
        delay(4000) 

        // Exit
        launch {
            scale.animateTo(
                targetValue = 1.2f, 
                animationSpec = tween(durationMillis = 300)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }

        delay(300)
        navController.navigate("dashboard") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Content - MikLink Logo at Center
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // MikLink Logo (Large)
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
                contentDescription = stringResource(R.string.splash_logo_description),
                modifier = Modifier.size(140.dp)
            )

            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

            // MikLink App Name
            androidx.compose.material3.Text(
                text = "MikLink",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }

        // Footer - Featured By Section
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .alpha(alpha.value)
        ) {
            // "Featured By"
            androidx.compose.material3.Text(
                text = "Featured By",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontFamily = JetBrainsMono,
                letterSpacing = 2.sp
            )

            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

            // GIF + SHITWORKS on same line
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Logo GIF (Small)
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(R.drawable.splash_logo)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = stringResource(R.string.splash_shitworks_logo_description),
                    modifier = Modifier.size(50.dp)
                )

                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))

                // "SHITWORKS" text
                androidx.compose.material3.Text(
                    text = "SHITWORKS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    fontFamily = JetBrainsMono
                )
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

            // Tagline
            androidx.compose.material3.Text(
                text = "'cause shit always works",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        // Loading Bar (just above footer)
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .width(200.dp)
                .alpha(alpha.value),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
        )
    }
}
