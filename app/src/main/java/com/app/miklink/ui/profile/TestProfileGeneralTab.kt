package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.testing.AgentUiTags

@Composable
internal fun TestProfileGeneralTab(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    profileDescription: String,
    onProfileDescriptionChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = profileName,
                onValueChange = onProfileNameChange,
                label = { Text(stringResource(R.string.profile_edit_name_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AgentUiTags.Profile.NAME),
                singleLine = true,
                isError = profileName.isBlank()
            )
        }
        item {
            OutlinedTextField(
                value = profileDescription,
                onValueChange = onProfileDescriptionChange,
                label = { Text(stringResource(R.string.profile_edit_description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AgentUiTags.Profile.DESCRIPTION)
            )
        }
    }
}
