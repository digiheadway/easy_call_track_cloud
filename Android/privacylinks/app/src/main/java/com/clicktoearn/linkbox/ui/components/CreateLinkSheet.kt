package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.local.AssetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLinkSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, expiryDays: Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expiryDays by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create Sharing Link",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Link Name") },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = expiryDays,
                onValueChange = { expiryDays = it.filter { c -> c.isDigit() } },
                label = { Text("Expiry (days)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Never") }
            )
            

            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onCreate(
                        name,
                        description,
                        expiryDays.toIntOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Link, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Link")
            }
        }
    }
}
