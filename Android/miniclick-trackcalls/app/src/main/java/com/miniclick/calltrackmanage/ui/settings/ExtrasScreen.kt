package com.miniclick.calltrackmanage.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Full-screen page for Extra/Utility settings.
 * Contains privacy, account, data management, and troubleshooting options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onResetOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Handle system back button
    androidx.activity.compose.BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extras") },
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
            // TROUBLESHOOTING
            // ===================================================================
            SettingsSection(title = "Troubleshooting") {
                ListItem(
                    headlineContent = { Text("Recheck Recordings") },
                    supportingContent = { 
                        Text("Force a re-scan of local folders for call recordings")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.AutoMirrored.Filled.ManageSearch, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.recheckRecordings()
                        Toast.makeText(context, "Rescanning recordings...", Toast.LENGTH_SHORT).show()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Re-import Call History") },
                    supportingContent = { 
                        Text("Re-fetch call logs from system and sync them")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.History, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.reImportCallHistory()
                        Toast.makeText(context, "Re-importing call history...", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Re-import Recordings") },
                    supportingContent = { 
                        Text("Reset recording sync status and retry all uploads")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CloudSync, MaterialTheme.colorScheme.tertiary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.reImportRecordings()
                        Toast.makeText(context, "Re-importing recordings...", Toast.LENGTH_SHORT).show()
                    }
                )

                // Cloud sync reset (if paired)
                if (uiState.pairingCode.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Reset Sync Status") },
                        supportingContent = { 
                            Text("Reset sync status of all logs - they will re-sync")
                        },
                        leadingContent = { 
                            SettingsIcon(Icons.Default.Sync, MaterialTheme.colorScheme.primary) 
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { 
                            viewModel.toggleResetConfirmDialog(true)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Force Sync Now") },
                        supportingContent = { 
                            Text("Manually trigger a sync with the server")
                        },
                        leadingContent = { 
                            SettingsIcon(Icons.Default.CloudUpload, MaterialTheme.colorScheme.secondary) 
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { 
                            viewModel.syncCallManually()
                            Toast.makeText(context, "Sync started...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // DIAGNOSTICS & SUPPORT
            // ===================================================================
            SettingsSection(title = "Diagnostics") {
                // App Permissions
                val grantedCount = uiState.permissions.count { it.isGranted }
                val totalCount = uiState.permissions.size
                ListItem(
                    headlineContent = { Text("App Permissions") },
                    supportingContent = { 
                        Text(
                            "$grantedCount of $totalCount permissions granted",
                            color = if (grantedCount == totalCount) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = { 
                        SettingsIcon(
                            if (grantedCount == totalCount) Icons.Default.CheckCircle else Icons.Default.Warning,
                            if (grantedCount == totalCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { viewModel.togglePermissionsModal(true) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Export Session Logs") },
                    supportingContent = { 
                        Text("Download and share app logs for support")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.HistoryEdu, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.exportLogs()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Reset Onboarding") },
                    supportingContent = { 
                        Text("View the initial app setup tutorial again")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Replay, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        onResetOnboarding()
                        viewModel.resetOnboarding()
                        Toast.makeText(context, "Onboarding reset. Restart app to see it.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // PRIVACY & LEGAL
            // ===================================================================
            SettingsSection(title = "Privacy & Legal") {
                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    supportingContent = { 
                        Text("Review how we handle your data")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Security, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://miniclickcrm.com/privacy"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Terms of Service") },
                    supportingContent = { 
                        Text("Review our terms and conditions")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Description, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://miniclickcrm.com/terms"))
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // ACCOUNT & DATA
            // ===================================================================
            SettingsSection(title = "Account & Data") {
                ListItem(
                    headlineContent = { Text("Delete Account") },
                    supportingContent = { 
                        Text("Request permanent deletion of your account and data")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.NoAccounts, MaterialTheme.colorScheme.error) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.toggleContactModal(true, "Account Deletion Request") 
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { 
                        Text("Clear All App Data", color = MaterialTheme.colorScheme.error) 
                    },
                    supportingContent = { 
                        Text("Delete all call logs, notes, and settings locally")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.DeleteForever, MaterialTheme.colorScheme.error) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.toggleClearDataDialog(true) 
                    }
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
