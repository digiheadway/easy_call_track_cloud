package com.miniclick.calltrackmanage.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.miniclick.calltrackmanage.ui.settings.SettingsUiState
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLookupResultModal(
    phoneNumber: String,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Automatically trigger lookup when modal opens
    LaunchedEffect(phoneNumber) {
        viewModel.fetchCustomLookupForPhone(phoneNumber)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearCustomLookupResponse()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Caller Data Lookup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phone: $phoneNumber",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Raw vs Formatted Toggle
                if (uiState.customLookupResponse != null && !uiState.isFetchingCustomLookup) {
                    Spacer(Modifier.height(16.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !uiState.isRawView,
                            onClick = { viewModel.toggleRawView(false) },
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        ) {
                            Text("Table", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = uiState.isRawView,
                            onClick = { viewModel.toggleRawView(true) },
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ) {
                            Text("Raw", fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))

            if (uiState.isFetchingCustomLookup) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.customLookupResponse != null) {
                val response = uiState.customLookupResponse!!
                
                if (uiState.isRawView) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = response,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    JsonTableView(json = response)
                }
                
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.fetchCustomLookupForPhone(phoneNumber) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Refresh")
                }
            } else {
                 Text("No data available. Make sure Custom Lookup is configured in Settings.")
            }
        }
    }
}

@Composable
fun JsonTableView(json: String) {
    val element = remember(json) {
        try {
            Gson().fromJson(json, JsonElement::class.java)
        } catch (e: Exception) {
            null
        }
    }

    if (element == null) {
        Text("Invalid JSON format", color = MaterialTheme.colorScheme.error)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
    ) {
        RenderJsonElement(element, "")
    }
}

@Composable
fun RenderJsonElement(element: JsonElement, key: String, level: Int = 0) {
    when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            if (key.isNotEmpty()) {
                JsonRow(key = key, value = "", level = level, isHeader = true)
            }
            obj.entrySet().forEach { entry ->
                RenderJsonElement(entry.value, entry.key, if (key.isEmpty()) level else level + 1)
            }
        }
        element.isJsonArray -> {
            val array = element.asJsonArray
            JsonRow(key = key, value = "[Array of ${array.size()}]", level = level, isHeader = true)
            array.forEachIndexed { index, item ->
                RenderJsonElement(item, "[$index]", level + 1)
            }
        }
        else -> {
            val value = when {
                element.isJsonNull -> "null"
                element.isJsonPrimitive -> {
                    val p = element.asJsonPrimitive
                    if (p.isString) p.asString else p.toString()
                }
                else -> element.toString()
            }
            JsonRow(key = key, value = value, level = level)
        }
    }
}

@Composable
fun JsonRow(key: String, value: String, level: Int, isHeader: Boolean = false) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Text(
                text = key,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (level * 12).dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            if (!isHeader) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth().padding(start = (level * 12).dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
