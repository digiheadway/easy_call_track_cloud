package com.miniclick.calltrackmanage.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AgreementScreen(
    onAccepted: () -> Unit
) {
    val context = LocalContext.current
    var isAgreed by remember { mutableStateOf(true) }
    
    val annotatedString = buildAnnotatedString {
        append("I have read and agree to the ")
        pushStringAnnotation(tag = "privacy", annotation = "https://miniclickcrm.com/privacy")
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
            append("Privacy Policy")
        }
        pop()
        append(" and ")
        pushStringAnnotation(tag = "terms", annotation = "https://miniclickcrm.com/terms")
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
            append("Terms of Service")
        }
        pop()
        append(".")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Terms & Privacy Policy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Before using MiniClick Calls, please review and accept our disclosure regarding your privacy and data handling.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))
            
            PermissionDisclosureItem(
                icon = Icons.Default.Call,
                title = "Core Communication Tools",
                description = "To handle calls, MiniClick requires access to your call log and contact information."
            )
            
            PermissionDisclosureItem(
                icon = Icons.Default.CloudSync,
                title = "Server Synchronization",
                description = "Your call logs can be synced to your organization's CRM for analytics and reporting."
            )
            
            PermissionDisclosureItem(
                icon = Icons.Default.Security,
                title = "Data Protection",
                description = "We use industry-standard encryption to protect your data during transfer and storage."
            )

            Spacer(Modifier.height(32.dp))

            // Checkbox and Agreement Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = { isAgreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset).firstOrNull()?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                            context.startActivity(intent)
                        }
                        annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset).firstOrNull()?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                            context.startActivity(intent)
                        }
                        if (annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset).isEmpty() &&
                            annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset).isEmpty()) {
                            isAgreed = !isAgreed
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onAccepted,
                enabled = isAgreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Accept & Continue", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}
