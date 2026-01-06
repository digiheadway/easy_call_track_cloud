package com.example.smsblaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.ui.components.*
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@Composable
fun TemplatesScreen(
    templates: List<Template>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateTemplate: () -> Unit,
    onTemplateClick: (Template) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SMSBlasterTheme.colors
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.gradientStart.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(top = 16.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Templates",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${templates.size} templates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    GradientButton(
                        text = "New",
                        onClick = onCreateTemplate,
                        icon = Icons.Default.Add,
                        modifier = Modifier.width(100.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SMSBlasterSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search templates..."
                )
            }
        }
        
        // Template list
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No Templates Yet",
                    description = "Create message templates with placeholders like {name} to personalize your SMS",
                    actionText = "Create Template",
                    onAction = onCreateTemplate
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = templates,
                    key = { it.id }
                ) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onTemplateClick(template) },
                        onDelete = { onDeleteTemplate(template) }
                    )
                }
            }
        }
    }
}
