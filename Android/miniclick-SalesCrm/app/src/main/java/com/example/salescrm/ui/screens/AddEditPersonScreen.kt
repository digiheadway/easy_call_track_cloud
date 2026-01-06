package com.example.salescrm.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salescrm.data.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime

// ==================== ADD/EDIT PERSON SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonScreen(
    person: Person?,
    isForPipeline: Boolean = false,
    defaultCountry: String = "US",
    currencySymbol: String = "$",
    prefillPhone: String? = null,
    prefillName: String? = null,
    onClose: () -> Unit,
    onSave: (Person) -> Unit,
    people: List<Person> = emptyList(),
    onManageSegments: () -> Unit = {},
    onManageSources: () -> Unit = {}
) {
    BackHandler { onClose() }
    
    // Country and Phone Logic
    val countryDialCodes = mapOf("US" to "+1", "IN" to "+91", "UK" to "+44", "CA" to "+1", "AU" to "+61")
    // Reverse lookup for initialization (Dial Code -> List of Country Codes)
    val dialCodeToCountries = countryDialCodes.entries.groupBy({ it.value }, { it.key })
    
    // Initialize phone and country
    // Priority: person?.phone -> prefillPhone -> empty
    val initialPhoneRaw = person?.phone ?: prefillPhone ?: ""
    var initialCountry = defaultCountry
    var initialNumber = initialPhoneRaw
    
    if (initialPhoneRaw.isNotEmpty()) {
        // Try to match specific dial codes
        // Sort by length desc to match +91 before +9 (if it existed)
        val sortedCodes = countryDialCodes.entries.sortedByDescending { it.value.length }
        for ((code, dial) in sortedCodes) {
            if (initialPhoneRaw.startsWith(dial)) {
                initialCountry = code // Or refine if multiple match?
                // If multiple countries share a code (like +1), checking if the current 'code' is the defaultCountry would be good preference
                // But for now, first match or simple logic suffices.
                // If we have "US" and "CA" for "+1". If raw starts with "+1":
                // If defaultCountry is "CA", pick "CA", else "US".
                
                val possibleCountries = dialCodeToCountries[dial] ?: emptyList()
                initialCountry = if (possibleCountries.contains(defaultCountry)) defaultCountry else possibleCountries.firstOrNull() ?: code
                
                initialNumber = initialPhoneRaw.removePrefix(dial).trim()
                break
            }
        }
    }

    // Essential form fields only
    var countryCode by remember { mutableStateOf(initialCountry) }
    var phoneNumber by remember { mutableStateOf(initialNumber) }
    // Priority: person?.name -> prefillName -> empty
    var name by remember { mutableStateOf(person?.name ?: prefillName ?: "") }
    var budget by remember { mutableStateOf(person?.budget ?: "") }
    var address by remember { mutableStateOf(person?.address ?: "") }
    var note by remember { mutableStateOf(person?.note ?: "") }
    var segmentId by remember { mutableStateOf(person?.segmentId ?: "new") }
    var sourceId by remember { mutableStateOf(person?.sourceId ?: "other") }
    
    // Pipeline toggle (determines if added to pipeline or contacts)
    var isInPipeline by remember { mutableStateOf(person?.isInPipeline ?: isForPipeline) }
    
    val focusManager = LocalFocusManager.current
    val phoneFocusRequester = remember { FocusRequester() }
    
    // Duplicate Phone Detection
    val currentFullPhone = if (phoneNumber.isNotBlank()) {
         val dialCode = countryDialCodes[countryCode] ?: ""
         "$dialCode $phoneNumber".trim()
    } else ""
    
    val duplicatePerson = remember(currentFullPhone, person, people) {
        if (currentFullPhone.isBlank()) null
        else {
            val normalizedCurrent = CallLogRepository.normalizePhoneNumber(currentFullPhone)
            if (normalizedCurrent.length < 5) null // Don't check tiny numbers
            else people.find { p ->
                p.id != person?.id && (
                    CallLogRepository.normalizePhoneNumber(p.phone) == normalizedCurrent ||
                    (p.alternativePhone.isNotBlank() && CallLogRepository.normalizePhoneNumber(p.alternativePhone) == normalizedCurrent)
                )
            }
        }
    }
    
    val hasDuplicateError = duplicatePerson != null
    val isValid = name.isNotBlank() && phoneNumber.isNotBlank() && !hasDuplicateError
    val isEditing = person != null
    val title = when {
        isEditing && isInPipeline -> "Edit Lead"
        isEditing -> "Edit Contact"
        isInPipeline -> "New Lead"
        else -> "New Contact"
    }
    
    // Only auto-focus phone when adding new person without prefilled data
    val shouldAutoFocusPhone = person == null && prefillPhone.isNullOrBlank()
    
    LaunchedEffect(Unit) { 
        if (shouldAutoFocusPhone) {
            phoneFocusRequester.requestFocus() 
        }
    }

    Scaffold(
        containerColor = SalesCrmTheme.colors.background,
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = SalesCrmTheme.colors.surface, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textMuted)
                    }
                    Text(
                        title, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    // Pipeline toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isInPipeline) "Lead" else "Contact",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isInPipeline) PrimaryBlue else AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = isInPipeline,
                            onCheckedChange = { isInPipeline = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryBlue,
                                checkedTrackColor = PrimaryBlue.copy(alpha = 0.3f),
                                uncheckedThumbColor = AccentGreen,
                                uncheckedTrackColor = AccentGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = SalesCrmTheme.colors.surface, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val fullPhone = if (phoneNumber.isNotBlank()) {
                             val dialCode = countryDialCodes[countryCode] ?: ""
                             "$dialCode $phoneNumber" // Store as "+1 123..."
                        } else ""
                        
                        onSave(Person(
                            id = person?.id ?: 0,
                            name = name,
                            phone = fullPhone.trim(),
                            alternativePhone = person?.alternativePhone ?: "",
                            address = address,
                            about = person?.about ?: "",
                            note = note,
                            labels = person?.labels ?: emptyList(),
                            segmentId = segmentId,
                            sourceId = sourceId,
                            budget = budget,
                            priorityId = person?.priorityId ?: "none",
                            isInPipeline = isInPipeline,
                            stageId = person?.stageId ?: "fresh",
                            pipelinePriorityId = person?.pipelinePriorityId ?: "medium",
                            createdAt = person?.createdAt ?: LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        ))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = isValid, 
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInPipeline) PrimaryBlue else AccentGreen,
                        disabledContainerColor = if (isInPipeline) PrimaryBlue.copy(alpha = 0.3f) else AccentGreen.copy(alpha = 0.3f)
                    )
                ) { 
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Add,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Save Changes" else if (isInPipeline) "Add Lead" else "Add Contact", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    ) 
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SalesCrmTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            
            // ===== PHONE (Primary - auto-focused) =====
            PhoneFormField(
                label = "Phone",
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it },
                placeholder = "98765 43210",
                isRequired = true,
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                focusRequester = phoneFocusRequester,
                isError = hasDuplicateError
            )
            
            if (hasDuplicateError) {
                Text(
                    text = "Phone number already exists for: ${duplicatePerson?.name}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // ===== NAME =====
            DarkFormField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                placeholder = "John Appleseed",
                isRequired = true,
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            // ===== BUDGET =====
            DarkFormField(
                label = "Budget",
                value = budget,
                onValueChange = { newValue ->
                    // Only allow digits
                    budget = newValue.filter { it.isDigit() }
                },
                placeholder = "Enter amount (numbers only)",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
            
            // Helper text for budget
            Text(
                "${currencySymbol} will be shown as prefix",
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // ===== ADDRESS =====
            DarkFormField(
                label = "Address",
                value = address,
                onValueChange = { address = it },
                placeholder = "123 Main St, City",
                singleLine = true,
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            // ===== MAIN NOTE =====
            DarkFormField(
                label = "Main Note",
                value = note,
                onValueChange = { note = it },
                placeholder = "Key requirements, interests, or important info...",
                singleLine = false,
                minLines = 3,
                imeAction = ImeAction.Done,
                onNext = { focusManager.clearFocus() }
            )
            
            Spacer(Modifier.height(24.dp))
            
            // ===== SEGMENT =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Segment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(
                    onClick = onManageSegments,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manage Segments",
                        tint = SalesCrmTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SegmentRow(
                selectedSegmentId = segmentId,
                onSegmentSelected = { segmentId = it }
            )
            
            Spacer(Modifier.height(20.dp))
            
            // ===== SOURCE =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Source",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(
                    onClick = onManageSources,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manage Sources",
                        tint = SalesCrmTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SourceRow(
                selectedSourceId = sourceId,
                onSourceSelected = { sourceId = it }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

// ==================== HELPER COMPONENTS ====================



@Composable
private fun SegmentRow(
    selectedSegmentId: String,
    onSegmentSelected: (String) -> Unit
) {
    val customSegments = SalesCrmTheme.segments
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(customSegments) { segmentItem ->
            val isSelected = segmentItem.id == selectedSegmentId
            val segmentColor = Color(segmentItem.color)
            
            Surface(
                modifier = Modifier.clickable { onSegmentSelected(segmentItem.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) segmentColor.copy(alpha = 0.2f) else SalesCrmTheme.colors.surfaceVariant,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) segmentColor else SalesCrmTheme.colors.border
                )
            ) {
                Text(
                    segmentItem.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSelected) segmentColor else SalesCrmTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SourceRow(
    selectedSourceId: String,
    onSourceSelected: (String) -> Unit
) {
    val customSources = SalesCrmTheme.sources
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(customSources) { sourceItem ->
            val isSelected = sourceItem.id == selectedSourceId
            val sourceColor = Color(sourceItem.color)
            
            Surface(
                modifier = Modifier.clickable { onSourceSelected(sourceItem.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) sourceColor.copy(alpha = 0.2f) else SalesCrmTheme.colors.surfaceVariant,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) sourceColor else SalesCrmTheme.colors.border
                )
            ) {
                Text(
                    sourceItem.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSelected) sourceColor else SalesCrmTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}
