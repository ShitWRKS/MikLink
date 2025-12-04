package com.app.miklink.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Displays a value with color coding and optional prefix
 *
 * @param value The value to display
 * @param modifier Modifier to be applied
 * @param isPositive Whether the value is positive (affects color)
 * @param prefix Optional prefix (e.g., "+", "$")
 * @param color Optional custom color
 */
@Composable
fun ValueDisplay(
    value: String,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    prefix: String = "",
    color: Color? = null
) {
    val displayColor = color ?: if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    
    Text(
        text = buildAnnotatedString {
            if (prefix.isNotEmpty()) {
                withStyle(style = SpanStyle(color = displayColor)) {
                    append(prefix)
                }
            }
            withStyle(
                style = SpanStyle(
                    color = displayColor,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(value)
            }
        },
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}
