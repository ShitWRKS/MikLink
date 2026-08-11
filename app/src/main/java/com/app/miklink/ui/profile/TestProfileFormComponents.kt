package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R

@Composable
internal fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                supportingText?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

@Composable
internal fun ProfileSwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    ListItem(
        headlineContent = { Text(headlineText) },
        supportingContent = supportingText?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun ThresholdRow(
    leftLabel: String,
    leftUnit: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    rightLabel: String? = null,
    rightUnit: String = "",
    rightValue: String = "",
    onRightChange: (String) -> Unit = {},
    leftIsError: Boolean = false,
    rightIsError: Boolean = false,
    leftErrorMessage: String? = null,
    rightErrorMessage: String? = null,
    leftTag: String? = null,
    rightTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        ThresholdField(
            label = leftLabel,
            unit = leftUnit,
            value = leftValue,
            onValueChange = onLeftChange,
            modifier = Modifier.weight(1f),
            isError = leftIsError,
            errorMessage = leftErrorMessage,
            semanticTag = leftTag
        )
        if (rightLabel != null) {
            ThresholdField(
                label = rightLabel,
                unit = rightUnit,
                value = rightValue,
                onValueChange = onRightChange,
                modifier = Modifier.weight(1f),
                isError = rightIsError,
                errorMessage = rightErrorMessage,
                semanticTag = rightTag
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ThresholdField(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    semanticTag: String? = null
) {
    val taggedModifier = if (semanticTag == null) modifier else modifier.testTag(semanticTag)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = taggedModifier,
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            {
                Text(
                    errorMessage ?: stringResource(
                        if (unit == "%") R.string.profile_edit_threshold_invalid_percentage
                        else R.string.profile_edit_threshold_invalid_number
                    )
                )
            }
        } else {
            null
        },
        suffix = { if (unit.isNotBlank()) Text(unit) }
    )
}
