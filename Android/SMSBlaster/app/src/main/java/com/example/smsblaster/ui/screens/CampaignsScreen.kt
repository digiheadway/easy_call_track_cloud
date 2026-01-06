package com.example.smsblaster.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.CampaignStatus
import com.example.smsblaster.ui.components.*
import com.example.smsblaster.ui.theme.SMSBlasterTheme
import com.example.smsblaster.viewmodel.CampaignWithTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignsScreen(
    campaignsWithTemplates: List<CampaignWithTemplate>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateCampaign: () -> Unit,
    onCampaignClick: (Campaign) -> Unit,
    onDeleteCampaign: (Campaign) -> Unit,
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
                            text = "Campaigns",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${campaignsWithTemplates.size} campaigns",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    GradientButton(
                        text = "New",
                        onClick = onCreateCampaign,
                        icon = Icons.Default.Add,
                        modifier = Modifier.width(100.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SMSBlasterSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search campaigns..."
                )
            }
        }
        
        // Campaign list
        if (campaignsWithTemplates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Campaign,
                    title = "No Campaigns Yet",
                    description = "Create your first campaign to start sending bulk SMS messages",
                    actionText = "Create Campaign",
                    onAction = onCreateCampaign
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
                    items = campaignsWithTemplates,
                    key = { it.campaign.id }
                ) { campaignWithTemplate ->
                    CampaignCard(
                        campaign = campaignWithTemplate.campaign,
                        templateName = campaignWithTemplate.template?.name,
                        onClick = { onCampaignClick(campaignWithTemplate.campaign) }
                    )
                }
            }
        }
    }
}

@Composable
fun CampaignStatusFilter(
    selectedStatus: CampaignStatus?,
    onStatusSelected: (CampaignStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(null) + CampaignStatus.entries
    val labels = listOf("All") + CampaignStatus.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEachIndexed { index, status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(labels[index]) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

