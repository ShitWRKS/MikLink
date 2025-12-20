/*
 * UI setup wizard card, input selection state for client/profile/socket, output guided step rendering.
 */
package com.app.miklink.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.theme.MikLinkShapeTokens
import com.app.miklink.ui.theme.MikLinkThemeTokens

@Composable
fun SetupWizardCard(
    clientName: String?,
    clientSubtitle: String?,
    profileName: String?,
    profileSubtitle: String?,
    socketValue: String,
    socketPlaceholder: String,
    modifier: Modifier = Modifier,
    onSelectClient: () -> Unit,
    onSelectProfile: () -> Unit,
    onSocketChange: (String) -> Unit,
    onManageClient: (() -> Unit)? = null,
    onManageProfile: (() -> Unit)? = null
) {
    val semantic = MikLinkThemeTokens.semantic
    val clientReady = !clientName.isNullOrBlank()
    val profileReady = !profileName.isNullOrBlank()
    val socketReady = socketValue.isNotBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_setup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            WizardStepRow(
                step = 1,
                title = stringResource(id = R.string.dashboard_section_client),
                value = clientName ?: stringResource(id = R.string.dashboard_select_client),
                subtitle = clientSubtitle,
                isComplete = clientReady,
                onClick = onSelectClient,
                onManage = onManageClient
            )
            WizardStepRow(
                step = 2,
                title = stringResource(id = R.string.dashboard_section_profile),
                value = profileName ?: stringResource(id = R.string.dashboard_select_profile),
                subtitle = profileSubtitle,
                isComplete = profileReady,
                onClick = onSelectProfile,
                onManage = onManageProfile
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.dashboard_section_socket),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = socketValue,
                    onValueChange = onSocketChange,
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Label, contentDescription = null)
                    },
                    placeholder = { Text(socketPlaceholder) },
                    shape = MikLinkShapeTokens.containedSmall,
                    isError = !socketReady,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = semantic.failure
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WizardStepRow(
    step: Int,
    title: String,
    value: String,
    subtitle: String?,
    isComplete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onManage: (() -> Unit)? = null
) {
    val semantic = MikLinkThemeTokens.semantic

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepBadge(step = step, isComplete = isComplete)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isComplete) {
                Text(
                    text = stringResource(id = R.string.dashboard_step_required),
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.failure
                )
            }
        }
        if (onManage != null) {
            IconButton(onClick = onManage) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StepBadge(step: Int, isComplete: Boolean) {
    val semantic = MikLinkThemeTokens.semantic
    val background = if (isComplete) semantic.success else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isComplete) semantic.onSuccess else MaterialTheme.colorScheme.onSurface
    Card(
        shape = MikLinkShapeTokens.containedSmall,
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}
