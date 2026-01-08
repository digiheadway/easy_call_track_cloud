package com.miniclick.calltrackmanage.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Full-screen page for Data Import/Export management.
 * Provides options for backing up and restoring app data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Handle system back button
    androidx.activity.compose.BackHandler {
        onBack()
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            viewModel.exportData(it)
            Toast.makeText(context, "Exporting data...", Toast.LENGTH_SHORT).show()
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            viewModel.importData(it)
            Toast.makeText(context, "Importing data...", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===================================================================
            // BACKUP / EXPORT
            // ===================================================================
            SettingsSection(title = "Backup") {
                ListItem(
                    headlineContent = { Text("Export All Data") },
                    supportingContent = { 
                        Column {
                            Text("Create a backup of all your data")
                            Text(
                                "Includes call logs, notes, labels, and settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Download, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        exportLauncher.launch("miniclick_backup_${System.currentTimeMillis()}.json")
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Share Backup") },
                    supportingContent = { 
                        Text("Export and share to email, cloud storage, etc.")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Share, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        // Export and then share
                        exportLauncher.launch("miniclick_backup_${System.currentTimeMillis()}.json")
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // RESTORE / IMPORT
            // ===================================================================
            SettingsSection(title = "Restore") {
                ListItem(
                    headlineContent = { Text("Import Data") },
                    supportingContent = { 
                        Column {
                            Text("Restore data from a backup file")
                            Text(
                                "Select a previously exported JSON file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Upload, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // INFO CARD
            // ===================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "About Backups",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your backup file contains:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        "• All call logs and their metadata",
                        "• Notes and labels for contacts",
                        "• App settings and preferences",
                        "• Excluded numbers list"
                    ).forEach { item ->
                        Text(
                            item,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Note: Recording files are not included in the backup. They remain on your device in the original location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
