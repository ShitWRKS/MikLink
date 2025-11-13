package com.app.miklink.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProfileListScreen(
    navController: NavController,
    viewModel: TestProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage Test Profiles") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("profile_edit/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(profiles) { profile ->
                ListItem(
                    headlineContent = { Text(profile.profileName) },
                    supportingContent = { Text(profile.profileDescription ?: "") },
                    modifier = Modifier.clickable {
                        navController.navigate("profile_edit/${profile.profileId}")
                    }
                )
            }
        }
    }
}
