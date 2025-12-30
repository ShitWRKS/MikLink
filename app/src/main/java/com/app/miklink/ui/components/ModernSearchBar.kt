package com.app.miklink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import com.app.miklink.ui.theme.CornerRadius
import com.app.miklink.ui.theme.Spacing
import com.app.miklink.R

/**
 * Modern search bar with filter button
 *
 * @param query Current search query
 * @param onQueryChange Callback when query changes
 * @param modifier Modifier to be applied
 * @param onFilterClick Callback when filter button is clicked
 * @param placeholder Placeholder text
 */
@Composable
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null,
    placeholder: String = "Search"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.medium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.common_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.sm)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
        
        onFilterClick?.let {
            IconButton(onClick = it) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.common_filter),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
