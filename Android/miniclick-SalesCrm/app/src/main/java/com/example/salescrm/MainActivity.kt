package com.example.salescrm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.example.salescrm.data.*
import com.example.salescrm.data.local.AppDatabase
import com.example.salescrm.notification.ReminderManager
import com.example.salescrm.ui.screens.*
import com.example.salescrm.ui.theme.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.util.*
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.compose.animation.core.tween
import android.provider.Settings
import android.content.Context
import android.app.AlarmManager

class MainActivity : ComponentActivity() {
    
    private lateinit var reminderManager: ReminderManager
    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var crmRepo: CrmRepository
    private lateinit var syncManager: FirestoreSyncManager
    
    // Permission request launcher - only schedules reminders, no chaining
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduleReminders()
        }
    }
    
    private var intentState = mutableStateOf<Intent?>(null)
    
    companion object {
        const val ACTION_ADD_LEAD = "com.example.salescrm.ACTION_ADD_LEAD"
        const val EXTRA_LEAD_NAME = "lead_name"
        const val EXTRA_LEAD_PHONE = "lead_phone"
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val processedIntent = processIncomingIntent(intent)
        intentState.value = processedIntent
    }
    
    /**
     * Process incoming intents from other apps to extract lead data.
     * Supports:
     * - ACTION_SEND with text/plain: Parses shared text to extract name and phone
     * - ACTION_ADD_LEAD: Custom action with lead_name and lead_phone extras
     * - Standard intents with prefill_phone and prefill_name extras
     */
    private fun processIncomingIntent(intent: Intent): Intent {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
                    
                    // Try to extract phone and name from the shared text
                    val (name, phone) = parseSharedText(sharedText, subject)
                    
                    // Create a new intent with the extracted data
                    return Intent(intent).apply {
                        putExtra("action", "add_lead")
                        if (phone.isNotBlank()) putExtra("prefill_phone", phone)
                        if (name.isNotBlank()) putExtra("prefill_name", name)
                    }
                }
            }
            ACTION_ADD_LEAD -> {
                val name = intent.getStringExtra(EXTRA_LEAD_NAME) ?: ""
                val phone = intent.getStringExtra(EXTRA_LEAD_PHONE) ?: ""
                
                return Intent(intent).apply {
                    putExtra("action", "add_lead")
                    if (phone.isNotBlank()) putExtra("prefill_phone", phone)
                    if (name.isNotBlank()) putExtra("prefill_name", name)
                }
            }
        }
        return intent
    }
    
    /**
     * Parse shared text to extract phone number and name.
     * Handles various formats:
     * - "Name: John Doe, Phone: 1234567890"
     * - "John Doe\n1234567890"
     * - Just a phone number
     * - Contact sharing format
     */
    private fun parseSharedText(text: String, subject: String): Pair<String, String> {
        var name = ""
        var phone = ""
        
        // Use subject as name if available
        if (subject.isNotBlank() && !subject.matches(Regex(".*\\d{7,}.*"))) {
            name = subject.trim()
        }
        
        // Phone number regex pattern (matches various formats)
        val phonePattern = Regex("""[\+]?[\d\s\-\(\)]{7,15}""")
        
        // Try to find phone number in text
        val phoneMatch = phonePattern.find(text)
        if (phoneMatch != null) {
            phone = phoneMatch.value.replace(Regex("[^+\\d]"), "")
        }
        
        // Try to extract name from text if not already found
        if (name.isBlank()) {
            // Check for "Name: xxx" or "Contact: xxx" patterns
            val namePatterns = listOf(
                Regex("""[Nn]ame[:\s]+([^\n,]+)"""),
                Regex("""[Cc]ontact[:\s]+([^\n,]+)""")
            )
            
            for (pattern in namePatterns) {
                val match = pattern.find(text)
                if (match != null && match.groupValues.size > 1) {
                    name = match.groupValues[1].trim()
                    break
                }
            }
            
            // If still no name, check if the text before the phone number could be a name
            if (name.isBlank() && phone.isNotBlank()) {
                val textBeforePhone = text.substringBefore(phoneMatch?.value ?: "").trim()
                // Check if it looks like a name (doesn't contain typical non-name patterns)
                if (textBeforePhone.isNotBlank() && 
                    textBeforePhone.length < 50 && 
                    !textBeforePhone.contains(Regex("[\\d@]"))) {
                    name = textBeforePhone.replace(Regex("[:\\-,]$"), "").trim()
                }
            }
        }
        
        return Pair(name, phone)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = processIncomingIntent(intent)
        enableEdgeToEdge()
        
        // Initialize Dependencies
        reminderManager = ReminderManager(this)
        userPrefs = UserPreferencesRepository(this)
        val database = AppDatabase.getDatabase(this)
        crmRepo = CrmRepository(database.salesDao())
        syncManager = FirestoreSyncManager(this, database.salesDao(), userPrefs)
        
        // Request notification permission and schedule reminders
        requestNotificationPermissionAndScheduleReminders()
        
        setContent {
            val themeMode by userPrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val currencySymbol by userPrefs.currencySymbol.collectAsState(initial = "$")
            val defaultCountry by userPrefs.defaultCountry.collectAsState(initial = "US")
            val budgetMultiplier by userPrefs.budgetMultiplier.collectAsState(initial = 1)
            val savedTaskViewMode by userPrefs.taskViewMode.collectAsState(initial = "DATE_WISE")
            val savedLastScreen by userPrefs.lastScreen.collectAsState(initial = "People")
            
            // Login state - Nullable to represent "Loading"
            val isLoggedInState = userPrefs.isLoggedIn.collectAsState(initial = null)
            val isLoggedIn = isLoggedInState.value
            val loggedInPhone by userPrefs.loggedInPhone.collectAsState(initial = "")
            
            // Custom items (Now from Room)
            val customStages by crmRepo.getCustomStages().collectAsState(initial = defaultCustomStages)
            val customPriorities by crmRepo.getCustomPriorities().collectAsState(initial = defaultCustomPriorities)
            val customSegments by crmRepo.getCustomSegments().collectAsState(initial = defaultCustomSegments)
            val customSources by crmRepo.getCustomSources().collectAsState(initial = defaultCustomSources)
            
            // CRM Data (Now from Room)
            val initialPeople by crmRepo.allPeople.collectAsState(initial = emptyList())
            val initialTasks by crmRepo.allTasks.collectAsState(initial = emptyList())
            val initialActivities by crmRepo.allActivities.collectAsState(initial = emptyList())
            
            // These still use DataStore for now as they are settings
            val initialCallSettings by userPrefs.callSettings.collectAsState(initial = CallSettings())
            
            // Check if data has been seeded to Room
            val hasSeededData by userPrefs.hasSeededData.collectAsState(initial = true)
            
            // Caller ID feature toggle
            val callerIdEnabled by userPrefs.callerIdEnabled.collectAsState(initial = false)
            val defaultWhatsAppPackage by userPrefs.defaultWhatsAppPackage.collectAsState(initial = "always_ask")
            
            // Data Migration Logic - Simplified: Just mark as seeded if it's a fresh install.
            // We don't want to pull old stale data from DataStore anymore.
            LaunchedEffect(hasSeededData) {
                if (!hasSeededData) {
                    userPrefs.setHasSeededData(true)
                }
            }
            
            // Start Firestore Sync when logged in
            LaunchedEffect(isLoggedIn, loggedInPhone) {
                if (isLoggedIn == true && loggedInPhone.isNotBlank()) {
                    syncManager.startSync(loggedInPhone)
                } else {
                    syncManager.stopSync()
                }
            }
            
            // Manage CallMonitorService based on Caller ID setting
            LaunchedEffect(callerIdEnabled) {
                if (callerIdEnabled) {
                    // Check if we have the required permissions before starting
                    val hasPhoneState = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPhoneState) {
                        com.example.salescrm.notification.CallMonitorService.startService(this@MainActivity)
                    }
                } else {
                    com.example.salescrm.notification.CallMonitorService.stopService(this@MainActivity)
                }
            }
            
            SalesCrmTheme(
                themeMode = themeMode,
                customStages = customStages,
                customPriorities = customPriorities,
                customSegments = customSegments,
                customSources = customSources
            ) { 
                if (isLoggedIn == null) {
                    // SPLASH SCREEN: Show while loading preference
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    AnimatedContent(
                        targetState = isLoggedIn,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "LoginTransition"
                    ) { loggedIn ->
                        if (!loggedIn) {
                            LoginScreen(
                                onLogin = { phone ->
                                    lifecycleScope.launch {
                                        userPrefs.login(phone)
                                        Toast.makeText(this@MainActivity, "Welcome!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else if (hasSeededData) {
                            val initialScreen = remember(savedLastScreen) {
                                when (savedLastScreen) {
                                    "Home" -> Screen.Home
                                    "People" -> Screen.People
                                    "Calls" -> Screen.Calls
                                    "Tasks" -> Screen.Tasks
                                    "Settings" -> Screen.Settings
                                    else -> Screen.People
                                }
                            }
                            
                            CrmApp(
                                initialScreen = initialScreen,
                                initialIntent = intentState.value,
                                onScreenChange = { screenName -> 
                                    lifecycleScope.launch { userPrefs.saveLastScreen(screenName) } 
                                },
                                currentThemeMode = themeMode,
                                currencySymbol = currencySymbol,
                                defaultCountry = defaultCountry,
                                budgetMultiplier = budgetMultiplier,
                                initialTaskViewMode = when (savedTaskViewMode) {
                                    "LIST" -> TaskViewMode.LIST
                                    "DATE_WISE" -> TaskViewMode.DATE_WISE
                                    else -> TaskViewMode.DATE_WISE
                                },
                                reminderManager = reminderManager,
                                customStages = customStages,
                                customPriorities = customPriorities,
                                customSegments = customSegments,
                                customSources = customSources,
                                initialPeople = initialPeople,
                                initialTasks = initialTasks,
                                initialActivities = initialActivities,
                                initialCallSettings = initialCallSettings,
                                onThemeModeChange = { newMode -> 
                                    lifecycleScope.launch { 
                                        userPrefs.setThemeMode(newMode) 
                                        Toast.makeText(this@MainActivity, "Theme updated", Toast.LENGTH_SHORT).show()
                                        // Sync config to cloud
                                        if (loggedInPhone.isNotBlank()) {
                                            syncManager.pushConfigToCloud(
                                                loggedInPhone,
                                                newMode.name,
                                                currencySymbol,
                                                defaultCountry,
                                                budgetMultiplier,
                                                savedTaskViewMode
                                            )
                                        }
                                    }
                                },
                                onCurrencyChange = { newCurrency ->
                                    lifecycleScope.launch { 
                                        userPrefs.setCurrencySymbol(newCurrency) 
                                        Toast.makeText(this@MainActivity, "Currency updated to $newCurrency", Toast.LENGTH_SHORT).show()
                                        // Sync config to cloud
                                        if (loggedInPhone.isNotBlank()) {
                                            syncManager.pushConfigToCloud(
                                                loggedInPhone,
                                                themeMode.name,
                                                newCurrency,
                                                defaultCountry,
                                                budgetMultiplier,
                                                savedTaskViewMode
                                            )
                                        }
                                    }
                                },
                                onDefaultCountryChange = { newCountry ->
                                    lifecycleScope.launch { 
                                        userPrefs.setDefaultCountry(newCountry) 
                                        Toast.makeText(this@MainActivity, "Default country updated", Toast.LENGTH_SHORT).show()
                                        // Sync config to cloud
                                        if (loggedInPhone.isNotBlank()) {
                                            syncManager.pushConfigToCloud(
                                                loggedInPhone,
                                                themeMode.name,
                                                currencySymbol,
                                                newCountry,
                                                budgetMultiplier,
                                                savedTaskViewMode
                                            )
                                        }
                                    }
                                },
                                onBudgetMultiplierChange = { newMultiplier ->
                                    lifecycleScope.launch { 
                                        userPrefs.setBudgetMultiplier(newMultiplier) 
                                        Toast.makeText(this@MainActivity, "Budget multiplier updated", Toast.LENGTH_SHORT).show()
                                        // Sync config to cloud
                                        if (loggedInPhone.isNotBlank()) {
                                            syncManager.pushConfigToCloud(
                                                loggedInPhone,
                                                themeMode.name,
                                                currencySymbol,
                                                defaultCountry,
                                                newMultiplier,
                                                savedTaskViewMode
                                            )
                                        }
                                    }
                                },
                                onTaskViewModeChange = { newMode ->
                                    lifecycleScope.launch { 
                                        userPrefs.setTaskViewMode(newMode.name)
                                        // Sync config to cloud
                                        if (loggedInPhone.isNotBlank()) {
                                            syncManager.pushConfigToCloud(
                                                loggedInPhone,
                                                themeMode.name,
                                                currencySymbol,
                                                defaultCountry,
                                                budgetMultiplier,
                                                newMode.name
                                            )
                                        }
                                    }
                                },
                                onDefaultWhatsAppPackageChange = { pkg ->
                                    lifecycleScope.launch { 
                                        userPrefs.setDefaultWhatsAppPackage(pkg)
                                        Toast.makeText(this@MainActivity, "WhatsApp preference updated", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCustomStagesChange = { stages ->
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomStages(stages) 
                                        Toast.makeText(this@MainActivity, "Pipeline stages saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCustomPrioritiesChange = { priorities ->
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomPriorities(priorities) 
                                        Toast.makeText(this@MainActivity, "Priorities saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCustomSegmentsChange = { segments ->
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomSegments(segments) 
                                        Toast.makeText(this@MainActivity, "Segments saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetCustomStages = {
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomStages(defaultCustomStages) 
                                        Toast.makeText(this@MainActivity, "Stages reset to default", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetCustomPriorities = {
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomPriorities(defaultCustomPriorities) 
                                        Toast.makeText(this@MainActivity, "Priorities reset to default", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetCustomSegments = {
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomSegments(defaultCustomSegments) 
                                        Toast.makeText(this@MainActivity, "Segments reset to default", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCustomSourcesChange = { sources ->
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomSources(sources) 
                                        Toast.makeText(this@MainActivity, "Sources saved", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetCustomSources = {
                                    lifecycleScope.launch { 
                                        crmRepo.saveCustomSources(defaultCustomSources) 
                                        Toast.makeText(this@MainActivity, "Sources reset to default", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPeopleChange = { updatedPeople ->
                                    // Here we should ideally compare and save only changed, 
                                    // but for stability during transition we can still use bulk save if we had it.
                                    // However, Room is better with individual saves.
                                    // Launching a migration to individual saves is safer.
                                    lifecycleScope.launch { 
                                        updatedPeople.forEach { crmRepo.savePerson(it) }
                                    }
                                },
                                onTasksChange = { updatedTasks ->
                                    lifecycleScope.launch { 
                                        updatedTasks.forEach { crmRepo.saveTask(it) }
                                    }
                                },
                                onActivitiesChange = { updatedActivities ->
                                    lifecycleScope.launch { 
                                        updatedActivities.forEach { crmRepo.saveActivity(it) }
                                    }
                                },
                                onCallSettingsChange = { updatedSettings ->
                                    lifecycleScope.launch { userPrefs.saveCallSettings(updatedSettings) }
                                },
                                onCallHistoryNoteChange = { phoneNumber, note ->
                                    // Notes are now inside CallHistoryEntry. 
                                    // This change will be handled by repository update calls if needed or directly.
                                },
                                loggedInPhone = loggedInPhone,
                                onLogout = {
                                    lifecycleScope.launch {
                                        userPrefs.logout()
                                        Toast.makeText(this@MainActivity, "Logged out", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onResetAllData = {
                                    lifecycleScope.launch {
                                        try {
                                            if (loggedInPhone.isNotBlank()) {
                                                try {
                                                    syncManager.clearCloudData(loggedInPhone)
                                                } catch (e: Exception) {
                                                    // Ignore cloud permission errors, proceed to clear local data
                                                    e.printStackTrace()
                                                }
                                            }
                                            crmRepo.clearAllData()
                                            userPrefs.logout()
                                            Toast.makeText(this@MainActivity, "Account reset successfully", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                crmRepository = crmRepo,
                                syncManager = syncManager,
                                callerIdEnabled = callerIdEnabled,
                                onCallerIdEnabledChange = { enabled ->
                                    lifecycleScope.launch {
                                        userPrefs.setCallerIdEnabled(enabled)
                                        if (enabled) {
                                            Toast.makeText(this@MainActivity, "Caller ID enabled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this@MainActivity, "Caller ID disabled", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                defaultWhatsAppPackage = defaultWhatsAppPackage
                            )
                        } else {
                            // Fallback loading while seeding
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryBlue)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun requestNotificationPermissionAndScheduleReminders() {
        // On startup, we only schedule reminders if notification permission is already granted
        // We don't ask for permissions upfront - they are requested just-in-time when features are used
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                scheduleReminders()
            }
            // Don't prompt for notification permission on startup
            // It will be requested when user creates a task with reminder
        } else {
            scheduleReminders()
        }
    }

    private fun scheduleReminders() {
        lifecycleScope.launch {
            // Re-schedule all reminders ensuring they are up to date
            val allTasks = crmRepo.getAllTasksSync()
            // Optimization: Fetch all people name/phone map
            val people = crmRepo.getAllPeopleSync()
            val peopleMap = people.associate { it.id to Pair(it.name, it.phone) }
            val defaultCountry = userPrefs.defaultCountry.first()
            
            reminderManager.scheduleAllReminders(allTasks, defaultCountry) { personId ->
                peopleMap[personId]
            }
        }
    }
}


// ==================== NAVIGATION ====================

sealed class Screen {
    data object Home : Screen()
    data object People : Screen()  // Unified Pipeline + Contacts
    data object Calls : Screen()   // Call log tracking
    data object Tasks : Screen()
    data object Settings : Screen()
    data class AddEditPerson(
        val personId: Int?, 
        val isForPipeline: Boolean,
        val prefillPhone: String? = null,
        val prefillName: String? = null
    ) : Screen()
}

data class NavItem(
    val title: String, 
    val selectedIcon: ImageVector, 
    val unselectedIcon: ImageVector,
    val screen: Screen
)

// ==================== MAIN APP ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmApp(
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    currencySymbol: String = "$",
    defaultCountry: String = "US",
    budgetMultiplier: Int = 1,
    initialTaskViewMode: TaskViewMode = TaskViewMode.DATE_WISE,
    initialScreen: Screen = Screen.People,
    onScreenChange: (String) -> Unit = {},
    reminderManager: ReminderManager? = null,
    customStages: List<CustomItem> = defaultCustomStages,
    customPriorities: List<CustomItem> = defaultCustomPriorities,
    customSegments: List<CustomItem> = defaultCustomSegments,
    customSources: List<CustomItem> = defaultCustomSources,
    initialPeople: List<Person> = emptyList(),
    initialTasks: List<Task> = emptyList(),
    initialActivities: List<Activity> = emptyList(),
    initialCallSettings: CallSettings = CallSettings(),
    loggedInPhone: String = "",
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onCurrencyChange: (String) -> Unit = {},
    onDefaultCountryChange: (String) -> Unit = {},
    onBudgetMultiplierChange: (Int) -> Unit = {},
    onTaskViewModeChange: (TaskViewMode) -> Unit = {},
    onCustomStagesChange: (List<CustomItem>) -> Unit = {},
    onCustomPrioritiesChange: (List<CustomItem>) -> Unit = {},
    onCustomSegmentsChange: (List<CustomItem>) -> Unit = {},
    onCustomSourcesChange: (List<CustomItem>) -> Unit = {},
    onResetCustomStages: () -> Unit = {},
    onResetCustomPriorities: () -> Unit = {},
    onResetCustomSegments: () -> Unit = {},
    onResetCustomSources: () -> Unit = {},
    onPeopleChange: (List<Person>) -> Unit = {},
    onTasksChange: (List<Task>) -> Unit = {},
    onActivitiesChange: (List<Activity>) -> Unit = {},
    onCallSettingsChange: (CallSettings) -> Unit = {},
    onCallHistoryNoteChange: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    crmRepository: CrmRepository? = null,
    syncManager: FirestoreSyncManager? = null,
    initialIntent: Intent? = null,
    callerIdEnabled: Boolean = false,
    onCallerIdEnabledChange: (Boolean) -> Unit = {},
    defaultWhatsAppPackage: String = "always_ask",
    onDefaultWhatsAppPackageChange: (String) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    var selectedNavIndex by remember { 
        mutableIntStateOf(
            when (initialScreen) {
                Screen.Home -> 0
                Screen.People -> 1
                Screen.Calls -> 2
                Screen.Tasks -> 3
                Screen.Settings -> 4
                else -> 1
            }
        ) 
    }
    var taskViewMode by remember(initialTaskViewMode) { mutableStateOf(initialTaskViewMode) }
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    
    // Unified data state - Use the passed-in lists directly from Room
    val people = initialPeople
    val tasks = initialTasks
    val activities = initialActivities
    var callSettings by remember(initialCallSettings) { mutableStateOf(initialCallSettings) }
    val scope = rememberCoroutineScope()
    val callLogRepository = remember { CallLogRepository(context) }

    // Audio Player State
    val audioPlayerState = rememberAudioPlayerState()
    
    // Call log state for detail sheet
    var viewingCallGroup by remember { mutableStateOf<CallLogGroup?>(null) }
    
    // Modal & Dialog states
    var selectedPersonId by remember { mutableStateOf<Int?>(null) } // For PersonDetailSheet
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var viewingTask by remember { mutableStateOf<Task?>(null) }
    var addingTaskForPerson by remember { mutableStateOf<Int?>(null) }
    var addingNoteForPerson by remember { mutableStateOf<Int?>(null) }
    var addingActivityForPerson by remember { mutableStateOf<Int?>(null) }
    var editingActivity by remember { mutableStateOf<Activity?>(null) }
    var showTogglePipelineConfirm by remember { mutableStateOf<Person?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    
    // Customization Sheet state
    var customizationSheetType by remember { mutableStateOf<CustomItemType?>(null) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    
    // Navigation History for Modal Returns
    var returnToPersonId by remember { mutableStateOf<Int?>(null) }
    var returnToTaskId by remember { mutableStateOf<Int?>(null) }
    
    // Navigation items
    val navItems = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, Screen.Home),
        NavItem("People", Icons.Filled.People, Icons.Outlined.People, Screen.People),
        NavItem("Calls", Icons.Filled.Phone, Icons.Outlined.Phone, Screen.Calls),
        NavItem("Tasks", Icons.Filled.DateRange, Icons.Outlined.DateRange, Screen.Tasks),
        NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, Screen.Settings)
    )
    
    val isFullScreenForm = currentScreen is Screen.AddEditPerson
    val showNavBars = !isFullScreenForm

    // Get the title for the top bar
    val topBarTitle = when (currentScreen) {
        Screen.Home -> "Home"
        Screen.People -> "" 
        Screen.Calls -> ""  // Calls has its own header
        Screen.Tasks -> "Tasks"
        Screen.Settings -> "Settings"
        else -> ""
    }

    // Handle incoming intents for navigation
    var processedIntent by remember { mutableStateOf<Intent?>(null) }
    
    LaunchedEffect(initialIntent, people) {
        val intent = initialIntent
        if (intent != null && intent != processedIntent) {
            val action = intent.getStringExtra("action")
            val openTask = intent.getBooleanExtra("open_task", false)
            val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
            val openPerson = intent.getBooleanExtra("open_person", false)
            val openPersonId = intent.getIntExtra("open_person_id", -1)
            val linkedPersonId = intent.getIntExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, -1)
            val prefillPhone = intent.getStringExtra("prefill_phone")
            val prefillName = intent.getStringExtra("prefill_name")

            when {
                action == "add_lead" || prefillPhone != null -> {
                    // Normalize the phone number for lookup
                    val normalizedPrefillPhone = prefillPhone?.let { CallLogRepository.normalizePhoneNumber(it) }
                    
                    // If we have a phone but people is empty, we might still be loading from Room.
                    // However, we can't wait forever. If seeded data is true, we should have something.
                    // We'll re-run this logic whenever 'people' changes until we mark it as processed.
                    
                    val existingPerson = if (normalizedPrefillPhone != null) {
                        people.find { person ->
                            CallLogRepository.normalizePhoneNumber(person.phone) == normalizedPrefillPhone ||
                            (person.alternativePhone.isNotBlank() && 
                             CallLogRepository.normalizePhoneNumber(person.alternativePhone) == normalizedPrefillPhone)
                        }
                    } else null
                    
                    if (existingPerson != null) {
                        // Person exists - show their profile instead
                        selectedPersonId = existingPerson.id
                        processedIntent = intent // Mark as processed
                    } else if (people.isNotEmpty() || prefillPhone == null) {
                        // Either we have people and no match, OR it's a lead without phone.
                        // Open add form.
                        selectedNavIndex = 1
                        currentScreen = Screen.AddEditPerson(
                            personId = null,
                            isForPipeline = true,
                            prefillPhone = prefillPhone,
                            prefillName = prefillName
                        )
                        processedIntent = intent // Mark as processed
                    }
                    // If people is empty and we have a phone, we wait for next emission of 'people'
                }
                action == "open_profile" && openPersonId != -1 -> {
                    selectedPersonId = openPersonId
                    processedIntent = intent
                }
                openPerson && linkedPersonId != -1 -> {
                    selectedPersonId = linkedPersonId
                    processedIntent = intent
                }
                openTask && taskId != -1 -> {
                    val task = tasks.find { it.id == taskId }
                    if (task != null) {
                        viewingTask = task
                        processedIntent = intent
                    }
                }
                else -> {
                    // Any other unrecognized intent or one we've decided to skip
                    processedIntent = intent
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        AnimatedVisibility(visible = !isFullScreenForm, enter = fadeIn(), exit = fadeOut()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = SalesCrmTheme.colors.background,
                topBar = {
                    if (showNavBars && topBarTitle.isNotEmpty()) {
                        SalesPipelineTopBar(
                            title = topBarTitle,
                            showTaskToggle = currentScreen is Screen.Tasks,
                            taskViewMode = taskViewMode,
                            onTaskViewModeChange = { 
                                taskViewMode = it
                                onTaskViewModeChange(it) // Persist the change
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (showNavBars && currentScreen != Screen.Home && currentScreen != Screen.Settings && currentScreen != Screen.Calls) {
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                when (currentScreen) {
                                    Screen.People -> currentScreen = Screen.AddEditPerson(null, isForPipeline = true)
                                    Screen.Tasks -> showAddTaskDialog = true
                                    else -> {}
                                }
                            },
                            containerColor = PrimaryBlue,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) { 
                            Icon(Icons.Default.Add, "Add") 
                        }
                    }
                },
                bottomBar = {
                    if (showNavBars) {
                        NavigationBar(
                            containerColor = SalesCrmTheme.colors.surface,
                            contentColor = SalesCrmTheme.colors.textPrimary
                        ) {
                            navItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedNavIndex == index,
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedNavIndex = index
                                        currentScreen = item.screen
                                        onScreenChange(
                                            when (item.screen) {
                                                Screen.Home -> "Home"
                                                Screen.People -> "People"
                                                Screen.Calls -> "Calls"
                                                Screen.Tasks -> "Tasks"
                                                Screen.Settings -> "Settings"
                                                else -> "People"
                                            }
                                        )
                                    },
                                    icon = { 
                                        Icon(
                                            if (selectedNavIndex == index) item.selectedIcon else item.unselectedIcon, 
                                            item.title
                                        ) 
                                    },
                                    label = { Text(item.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = SalesCrmTheme.colors.textMuted,
                                        unselectedTextColor = SalesCrmTheme.colors.textMuted,
                                        indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            people = people,
                            tasks = tasks,
                            activities = activities,
                            onViewTask = { viewingTask = it },
                            onPersonClick = { selectedPersonId = it },
                            isSyncing = isSyncing,
                            onSync = {
                                if (loggedInPhone.isNotBlank() && syncManager != null) {
                                    isSyncing = true
                                    syncManager.forceFullSync(loggedInPhone) { success ->
                                        isSyncing = false
                                        android.widget.Toast.makeText(
                                            context, 
                                            if (success) "Sync completed" else "Sync failed", 
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Not logged in", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        
                        Screen.People -> PeopleScreen(
                            people = people,
                            onPersonClick = { selectedPersonId = it.id },
                            onAddPerson = { isForPipeline -> 
                                currentScreen = Screen.AddEditPerson(null, isForPipeline = isForPipeline) 
                            },
                            currencySymbol = currencySymbol,
                            budgetMultiplier = budgetMultiplier
                        )
                        
                        Screen.Calls -> CallsScreen(
                            people = people,
                            onAddActivity = { phoneNumber, content ->
                                // Optional: Handle general activity
                            },
                            onAddToPipeline = { phoneNumber, contactName ->
                                // Create a new person from the phone number and navigate to add screen with prefilled data
                                currentScreen = Screen.AddEditPerson(
                                    personId = null, 
                                    isForPipeline = true,
                                    prefillPhone = phoneNumber,
                                    prefillName = contactName
                                )
                                android.widget.Toast.makeText(context, "Add ${contactName ?: phoneNumber} to pipeline", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onPersonClick = { selectedPersonId = it.id },
                            onViewCallLog = { group ->
                                viewingCallGroup = group
                            },
                            callSettings = callSettings,
                            onUpdateSettings = { 
                                callSettings = it
                                onCallSettingsChange(it)
                            },
                            onImportPastCalls = { onProgress ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val currentPeople = people.toList()
                                        val total = currentPeople.size
                                        if (total == 0) {
                                            withContext(Dispatchers.Main) { onProgress(1f) }
                                            return@withContext
                                        }
                                        
                                        currentPeople.forEachIndexed { index, person ->
                                            val newLogs = callLogRepository.syncCallHistoryForPerson(
                                                person = person,
                                                settings = callSettings,
                                                existingLogs = activities
                                            )
                                            if (newLogs.isNotEmpty()) {
                                                newLogs.forEach { onActivitiesChange(listOf(it)) }
                                            }
                                            withContext(Dispatchers.Main) {
                                                onProgress((index + 1).toFloat() / total)
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            onProgress(1f)
                                            android.widget.Toast.makeText(context, "Call history imported for ${currentPeople.size} leads", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            crmRepository = crmRepository!!
                        )
                        
                        Screen.Tasks -> TasksScreen(
                            tasks = tasks,
                            people = people,
                            onTaskClick = { viewingTask = it },
                            onToggleTask = { task ->
                                val newStatus = if (task.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING
                                val updatedTask = task.copy(
                                    status = newStatus,
                                    completedAt = if (task.status == TaskStatus.PENDING) LocalDateTime.now() else null
                                )
                                onTasksChange(listOf(updatedTask))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                android.widget.Toast.makeText(context, if (newStatus == TaskStatus.COMPLETED) "Task completed" else "Task pending", android.widget.Toast.LENGTH_SHORT).show()

                                // Cancel reminder if task is completed
                                if (newStatus == TaskStatus.COMPLETED) {
                                    reminderManager?.cancelReminder(task.id)
                                } else {
                                    // Reschedule if uncompleted
                                    val linkedPerson = people.find { it.id == updatedTask.linkedPersonId }
                                    reminderManager?.scheduleReminder(updatedTask, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                                }
                            },
                            onAddTask = { showAddTaskDialog = true },
                            viewMode = taskViewMode,
                            onViewModeChange = { taskViewMode = it },
                            onPersonClick = { selectedPersonId = it.id }
                        )
                        
                        Screen.Settings -> SettingsScreen(
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange,
                            currencySymbol = currencySymbol,
                            onCurrencyChange = onCurrencyChange,
                            defaultCountry = defaultCountry,
                            onDefaultCountryChange = onDefaultCountryChange,
                            budgetMultiplier = budgetMultiplier,
                            onBudgetMultiplierChange = onBudgetMultiplierChange,
                            customStages = customStages,
                            customPriorities = customPriorities,
                            customSegments = customSegments,
                            customSources = customSources,
                            onEditStages = { customizationSheetType = CustomItemType.STAGES },
                            onEditPriorities = { customizationSheetType = CustomItemType.PRIORITIES },
                            onEditSegments = { customizationSheetType = CustomItemType.SEGMENTS },
                            onEditSources = { customizationSheetType = CustomItemType.SOURCES },
                            onNavigateToPermissions = { showPermissionsSheet = true },
                            loggedInPhone = loggedInPhone,
                            onLogout = onLogout,
                            onResetAllData = onResetAllData,
                            callerIdEnabled = callerIdEnabled,
                            onCallerIdEnabledChange = onCallerIdEnabledChange,
                            defaultWhatsAppPackage = defaultWhatsAppPackage,
                            onDefaultWhatsAppPackageChange = onDefaultWhatsAppPackageChange
                        )

                        else -> {}
                    }
                }
            }
        }
        
        // Full-screen Add/Edit Person form
        AnimatedVisibility(
            visible = currentScreen is Screen.AddEditPerson,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val screen = currentScreen as? Screen.AddEditPerson
            screen?.let { s ->
                val person = s.personId?.let { id -> people.find { it.id == id } }
                AddEditPersonScreen(
                    person = person,
                    isForPipeline = s.isForPipeline,
                    defaultCountry = defaultCountry,
                    currencySymbol = currencySymbol,
                    prefillPhone = s.prefillPhone,
                    prefillName = s.prefillName,
                    people = people,
                    onClose = { 
                        // Return to previous state
                        currentScreen = Screen.People
                        
                        // If we came from the detail modal, re-open it
                        if (returnToPersonId != null && returnToPersonId == s.personId) {
                            selectedPersonId = returnToPersonId
                        }
                        returnToPersonId = null
                    },
                    onSave = { updated ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (s.personId != null) {
                            onPeopleChange(listOf(updated))
                            onActivitiesChange(listOf(Activity(
                                personId = updated.id, 
                                type = ActivityType.SYSTEM,
                                title = "Updated", 
                                description = "Person details updated"
                            )))
                            android.widget.Toast.makeText(context, "Saved changes", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            // Last-minute duplicate check for safety
                            val normalized = CallLogRepository.normalizePhoneNumber(updated.phone)
                            val duplicate = people.find { CallLogRepository.normalizePhoneNumber(it.phone) == normalized }
                            
                            if (duplicate != null) {
                                android.widget.Toast.makeText(context, "Cannot add: Phone already exists!", android.widget.Toast.LENGTH_LONG).show()
                                return@AddEditPersonScreen
                            }
                            
                            // Use id = 0, Room will auto-generate
                            onPeopleChange(listOf(updated))
                            android.widget.Toast.makeText(context, "Lead added", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        currentScreen = Screen.People
                        // Only open detail view for edits (where we have the ID)
                        // For new people, id is 0 and Room will auto-generate, so we can't open detail immediately
                        if (s.personId != null) {
                            selectedPersonId = updated.id
                        }
                        returnToPersonId = null
                    },
                    onManageSegments = { customizationSheetType = CustomItemType.SEGMENTS },
                    onManageSources = { customizationSheetType = CustomItemType.SOURCES }
                )
            }
        }
        
        // Overlay Audio Player (Inside Box scope)
        if (audioPlayerState.playingPath != null) {
            GlobalAudioPlayer(
                state = audioPlayerState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showNavBars) 80.dp else 20.dp)
                    .zIndex(100f)
            )
        }
    }

    // ===== MODALS & DIALOGS =====

    // Person Detail Sheet (Modal)
    selectedPersonId?.let { personId ->
        val person = people.find { it.id == personId }
        if (person != null) {
            PersonDetailSheet(
                person = person,
                tasks = tasks,
                activities = activities,
                currencySymbol = currencySymbol,
                budgetMultiplier = budgetMultiplier,
                defaultCountry = defaultCountry,
                onDismiss = { selectedPersonId = null },
                onEdit = {
                    returnToPersonId = person.id
                    selectedPersonId = null // Close modal to go to full screen edit
                    currentScreen = Screen.AddEditPerson(person.id, person.isInPipeline)
                },
                onStageChange = { stageId ->
                    val stageLabel = customStages.findById(stageId)?.label ?: "Unknown"
                    onPeopleChange(listOf(person.copy(stageId = stageId, updatedAt = LocalDateTime.now())))
                    onActivitiesChange(listOf(Activity(personId = person.id, type = ActivityType.SYSTEM, title = "Stage Changed", description = "Moved to $stageLabel")))
                    android.widget.Toast.makeText(context, "Stage: $stageLabel", android.widget.Toast.LENGTH_SHORT).show()
                },
                onPriorityChange = { pId ->
                    val updated = if (person.isInPipeline) {
                        person.copy(pipelinePriorityId = pId, updatedAt = LocalDateTime.now())
                    } else {
                        person.copy(priorityId = pId, updatedAt = LocalDateTime.now())
                    }
                    onPeopleChange(listOf(updated))
                    android.widget.Toast.makeText(context, "Priority updated", android.widget.Toast.LENGTH_SHORT).show()
                },
                onSegmentChange = { sId ->
                    onPeopleChange(listOf(person.copy(segmentId = sId, updatedAt = LocalDateTime.now())))
                },
                onLabelAdd = { label ->
                    val newLabels = person.labels.toMutableList().apply { add(label) }
                    onPeopleChange(listOf(person.copy(labels = newLabels, updatedAt = LocalDateTime.now())))
                    android.widget.Toast.makeText(context, "Label added", android.widget.Toast.LENGTH_SHORT).show()
                },
                onLabelRemove = { label ->
                    val newLabels = person.labels.toMutableList().apply { remove(label) }
                    onPeopleChange(listOf(person.copy(labels = newLabels, updatedAt = LocalDateTime.now())))
                    android.widget.Toast.makeText(context, "Label removed", android.widget.Toast.LENGTH_SHORT).show()
                },
                onNoteChange = { noteContent ->
                    onPeopleChange(listOf(person.copy(note = noteContent, updatedAt = LocalDateTime.now())))
                    android.widget.Toast.makeText(context, "Note updated", android.widget.Toast.LENGTH_SHORT).show()
                },
                onAddTask = { addingTaskForPerson = person.id },
                onTaskClick = { viewingTask = it },
                onToggleTask = { task ->
                    val newStatus = if (task.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING
                    val updatedTask = task.copy(
                        status = newStatus,
                        completedAt = if (task.status == TaskStatus.PENDING) LocalDateTime.now() else null
                    )
                    onTasksChange(listOf(updatedTask))
                    android.widget.Toast.makeText(context, if (newStatus == TaskStatus.COMPLETED) "Task completed" else "Task pending", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // Cancel reminder if task is completed
                    if (newStatus == TaskStatus.COMPLETED) {
                        reminderManager?.cancelReminder(task.id)
                    } else {
                        // Reschedule if uncompleted
                        val linkedPerson = people.find { it.id == task.linkedPersonId }
                        reminderManager?.scheduleReminder(updatedTask, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                    }
                },
                onAddComment = { addingNoteForPerson = person.id },
                onCommentChange = { comment, content ->
                     onActivitiesChange(listOf(comment.copy(description = content)))
                     android.widget.Toast.makeText(context, "Comment updated", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCommentDelete = { comment ->
                    // Trigger Cloud delete
                    if (loggedInPhone.isNotBlank()) {
                        syncManager?.deleteActivityCloud(loggedInPhone, comment.id)
                    }
                    // Delete from Room
                    scope.launch { crmRepository?.deleteActivity(comment) }
                    android.widget.Toast.makeText(context, "Comment deleted", android.widget.Toast.LENGTH_SHORT).show()
                },
                onBudgetChange = { newBudget ->
                     onPeopleChange(listOf(person.copy(budget = newBudget, updatedAt = LocalDateTime.now())))
                     android.widget.Toast.makeText(context, "Budget updated", android.widget.Toast.LENGTH_SHORT).show()
                },
                onTogglePipeline = {
                    showTogglePipelineConfirm = person
                },
                onDeletePerson = {
                    val personToDelete = person
                    // Trigger Cloud delete
                    if (loggedInPhone.isNotBlank()) {
                        syncManager?.deletePersonCloud(loggedInPhone, person.id)
                    }

                    scope.launch {
                        crmRepository?.deletePerson(personToDelete)
                    }
                    
                    android.widget.Toast.makeText(context, "Person deleted", android.widget.Toast.LENGTH_SHORT).show()
                    selectedPersonId = null
                },
                onCall = {
                    onActivitiesChange(listOf(Activity(personId = person.id, type = ActivityType.SYSTEM, title = "Call", description = "Started a call")))
                },
                onWhatsApp = {
                    onActivitiesChange(listOf(Activity(personId = person.id, type = ActivityType.SYSTEM, title = "WhatsApp", description = "Started WhatsApp chat")))
                    try { context.startActivity(CallLogRepository.createWhatsAppChooserIntent(context, person.phone, defaultWhatsAppPackage)) } catch (e: Exception) {}
                },
                onAddActivity = {
                    addingActivityForPerson = person.id
                },
                onActivityClick = { activity ->
                    editingActivity = activity
                },
                defaultWhatsAppPackage = defaultWhatsAppPackage
            )
        } else {
            selectedPersonId = null
        }
    }

    // View Task Detail Sheet
    viewingTask?.let { task ->
        val linkedPerson = task.linkedPersonId?.let { id -> people.find { it.id == id } }
        var taskResponse by remember { mutableStateOf(task.response) }
        
        TaskDetailSheet(
            task = task,
            linkedPerson = linkedPerson,
            currentResponse = taskResponse,
            currencySymbol = currencySymbol,
            budgetMultiplier = budgetMultiplier,
            onDismiss = { viewingTask = null },
            onEdit = { 
                editingTask = task
                returnToTaskId = task.id
                viewingTask = null 
            },
            onMarkDone = {
                val newStatus = if (task.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING
                val updatedTask = task.copy(
                    status = newStatus,
                    completedAt = if (task.status == TaskStatus.PENDING) LocalDateTime.now() else null,
                    response = taskResponse
                )
                onTasksChange(listOf(updatedTask))
                android.widget.Toast.makeText(context, if (newStatus == TaskStatus.COMPLETED) "Task completed" else "Task pending", android.widget.Toast.LENGTH_SHORT).show()
                
                // Cancel reminder if task is completed
                if (newStatus == TaskStatus.COMPLETED) {
                    reminderManager?.cancelReminder(task.id)
                } else {
                    // Reschedule if uncompleted
                    val linkedPerson = people.find { it.id == task.linkedPersonId }
                    reminderManager?.scheduleReminder(updatedTask, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                }
                
                // Save response as comment if not empty and newly completed
                if (taskResponse.isNotBlank() && task.linkedPersonId != null && newStatus == TaskStatus.COMPLETED) {
                    onActivitiesChange(listOf(Activity(
                        personId = task.linkedPersonId!!,
                        type = ActivityType.COMMENT,
                        description = "Task Response: $taskResponse",
                        timestamp = LocalDateTime.now()
                    )))
                }
                
                viewingTask = null 
            },
            onReschedule = {},
            onTaskUpdate = { updated ->
                onTasksChange(listOf(updated))
                // Reschedule Reminder
                reminderManager?.cancelReminder(updated.id)
                if (updated.status == TaskStatus.PENDING) {
                    val linkedPerson = people.find { it.id == updated.linkedPersonId }
                    reminderManager?.scheduleReminder(updated, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                }
                
                android.widget.Toast.makeText(context, "Task updated", android.widget.Toast.LENGTH_SHORT).show()
                viewingTask = updated
            },
            onDelete = {
                // Cancel the reminder
                reminderManager?.cancelReminder(task.id)
                // Trigger Cloud delete
                if (loggedInPhone.isNotBlank()) {
                    syncManager?.deleteTaskCloud(loggedInPhone, task.id)
                }
                // Delete from Room
                scope.launch { crmRepository?.deleteTask(task) }
                android.widget.Toast.makeText(context, "Task deleted", android.widget.Toast.LENGTH_SHORT).show()
                viewingTask = null
            },
            onResponseChange = { response ->
                taskResponse = response
                // Auto-save: Save as note when response changes (debounced in real app)
                // For now, we'll save it when marking as done or closing
            },
            onCallPerson = { p ->
                try {
                    val dialPhone = CallLogRepository.formatPhoneForDialer(p.phone, defaultCountry)
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialPhone")))
                    onActivitiesChange(listOf(Activity(personId = p.id, type = ActivityType.SYSTEM, title = "Call", description = "Started a call via task")))
                } catch (e: Exception) {}
            },
            onWhatsAppPerson = { p ->
                try {
                    context.startActivity(CallLogRepository.createWhatsAppChooserIntent(context, p.phone, defaultWhatsAppPackage))
                    onActivitiesChange(listOf(Activity(personId = p.id, type = ActivityType.SYSTEM, title = "WhatsApp", description = "Started WhatsApp via task")))
                } catch (e: Exception) {}
            },
            onViewPerson = { person ->
                // Open the person detail sheet
                viewingTask = null
                selectedPersonId = person.id
            }
        )
    }
    
    // Call Log Detail Sheet
    viewingCallGroup?.let { group ->
        CallLogDetailSheet(
            group = group,
            onDismiss = { viewingCallGroup = null },
            onAddNote = { content ->
                // Note is stored directly in CallLogEntry now
                android.widget.Toast.makeText(context, "Note added to call", android.widget.Toast.LENGTH_SHORT).show()
            },
            onAddToPipeline = {
                viewingCallGroup = null
                currentScreen = Screen.AddEditPerson(
                    personId = null, 
                    isForPipeline = true,
                    prefillPhone = group.phoneNumber,
                    prefillName = group.displayName.takeIf { it != group.phoneNumber }
                )
                android.widget.Toast.makeText(context, "Add ${group.displayName} to pipeline", android.widget.Toast.LENGTH_SHORT).show()
            },
            onViewPerson = {
                group.linkedPersonId?.let { id ->
                    viewingCallGroup = null
                    selectedPersonId = id
                }
            },
            onCall = {
                try {
                    val dialPhone = CallLogRepository.formatPhoneForDialer(group.phoneNumber, defaultCountry)
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialPhone")))
                } catch (e: Exception) {}
            }
        )
    }

    // Add Task Dialog (standalone)
    if (showAddTaskDialog) {
        AddEditTaskSheet(
            task = null,
            linkedPersonId = null,
            people = people,
            onDismiss = { showAddTaskDialog = false },
            onSave = { task ->
                onTasksChange(listOf(task))
                // Schedule reminder for the new task
                val linkedPerson = task.linkedPersonId?.let { pId -> people.find { it.id == pId } }
                reminderManager?.scheduleReminder(task, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                android.widget.Toast.makeText(context, "Task added", android.widget.Toast.LENGTH_SHORT).show()
                showAddTaskDialog = false
            }
        )
    }
    
    // Add Task for Person Dialog
    addingTaskForPerson?.let { personId ->
        AddEditTaskSheet(
            task = null,
            linkedPersonId = personId,
            people = people,
            onDismiss = { addingTaskForPerson = null },
            onSave = { task ->
                onTasksChange(listOf(task))
                // Schedule reminder for the new task
                val linkedPerson = task.linkedPersonId?.let { pId -> people.find { it.id == pId } }
                reminderManager?.scheduleReminder(task, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                onActivitiesChange(listOf(Activity(personId = personId, type = ActivityType.SYSTEM, title = "Task Added", description = task.type.label)))
                android.widget.Toast.makeText(context, "Task added", android.widget.Toast.LENGTH_SHORT).show()
                addingTaskForPerson = null
            }
        )
    }
    
    // Edit Task Dialog
    editingTask?.let { task ->
        AddEditTaskSheet(
            task = task,
            linkedPersonId = task.linkedPersonId,
            people = people,
            onDismiss = { 
                editingTask = null 
                // Restore viewing task if we came from detail
                if (returnToTaskId != null) {
                    viewingTask = tasks.find { it.id == returnToTaskId }
                    returnToTaskId = null
                }
            },
            onSave = { updated ->
                val idx = tasks.indexOfFirst { it.id == updated.id }
                onTasksChange(listOf(updated))
                // Reschedule reminder for the updated task
                reminderManager?.cancelReminder(updated.id)
                val linkedPerson = people.find { it.id == updated.linkedPersonId }
                reminderManager?.scheduleReminder(updated, linkedPerson?.name, linkedPerson?.phone, defaultCountry)
                android.widget.Toast.makeText(context, "Task updated", android.widget.Toast.LENGTH_SHORT).show()
                editingTask = null
                
                // Restore viewing task
                if (returnToTaskId != null) {
                    viewingTask = tasks.find { it.id == returnToTaskId }
                    returnToTaskId = null
                }
            }
        )
    }
    
    // Add Comment Sheet
    addingNoteForPerson?.let { personId ->
        AddNoteSheet(
            personId = personId,
            onDismiss = { addingNoteForPerson = null },
            onSave = { comment ->
                onActivitiesChange(listOf(comment))
                android.widget.Toast.makeText(context, "Comment added", android.widget.Toast.LENGTH_SHORT).show()
                addingNoteForPerson = null
            }
        )
    }
    
    // Add Activity Sheet
    addingActivityForPerson?.let { personId ->
        AddActivitySheet(
            personId = personId,
            onDismiss = { addingActivityForPerson = null },
            onSave = { activity ->
                onActivitiesChange(listOf(activity))
                android.widget.Toast.makeText(context, "Activity logged", android.widget.Toast.LENGTH_SHORT).show()
                addingActivityForPerson = null
            }
        )
    }
    
    // Edit Activity Sheet
    editingActivity?.let { activity ->
        EditActivitySheet(
            activity = activity,
            onDismiss = { editingActivity = null },
            onSave = { updatedActivity ->
                onActivitiesChange(listOf(updatedActivity))
                android.widget.Toast.makeText(context, "Activity updated", android.widget.Toast.LENGTH_SHORT).show()
                editingActivity = null
            },
            onDelete = {
                // Delete from Room
                scope.launch { crmRepository?.deleteActivity(activity) }
                android.widget.Toast.makeText(context, "Activity deleted", android.widget.Toast.LENGTH_SHORT).show()
                editingActivity = null
            }
        )
    }
    
    // Toggle Pipeline Confirm Sheet
    showTogglePipelineConfirm?.let { person ->
        val newStatus = !person.isInPipeline
        ConfirmSheet(
            title = if (newStatus) "Add to Pipeline?" else "Move to Contacts?",
            message = if (newStatus) "This will add ${person.name} to the sales pipeline." else "This will remove ${person.name} from the pipeline and move them to contacts.",
            confirmText = if (newStatus) "Add to Pipeline" else "Move to Contacts",
            isDestructive = !newStatus,
            onConfirm = {
                val updatedPerson = person.copy(
                    isInPipeline = newStatus,
                    stageId = if (newStatus) "fresh" else person.stageId,
                    updatedAt = LocalDateTime.now()
                )
                onPeopleChange(listOf(updatedPerson))
                onActivitiesChange(listOf(Activity(personId = person.id, type = ActivityType.SYSTEM, title = if (newStatus) "Added to Pipeline" else "Moved to Contacts", description = if (newStatus) "Stage: Fresh" else "Removed from pipeline")))
                android.widget.Toast.makeText(context, if (newStatus) "Added to pipeline" else "Moved to contacts", android.widget.Toast.LENGTH_SHORT).show()
                showTogglePipelineConfirm = null
            },
            onDismiss = { showTogglePipelineConfirm = null }
        )
    }

    // ==================== CUSTOMIZATION SHEETS ====================
    customizationSheetType?.let { type ->
        CustomItemsSheet(
            type = type,
            items = when (type) {
                CustomItemType.STAGES -> customStages
                CustomItemType.PRIORITIES -> customPriorities
                CustomItemType.SEGMENTS -> customSegments
                CustomItemType.SOURCES -> customSources
            },
            onItemsChange = { newList ->
                when (type) {
                    CustomItemType.STAGES -> onCustomStagesChange(newList)
                    CustomItemType.PRIORITIES -> onCustomPrioritiesChange(newList)
                    CustomItemType.SEGMENTS -> onCustomSegmentsChange(newList)
                    CustomItemType.SOURCES -> onCustomSourcesChange(newList)
                }
            },
            onReset = {
                when (type) {
                    CustomItemType.STAGES -> onResetCustomStages()
                    CustomItemType.PRIORITIES -> onResetCustomPriorities()
                    CustomItemType.SEGMENTS -> onResetCustomSegments()
                    CustomItemType.SOURCES -> onResetCustomSources()
                }
            },
            onDismiss = { customizationSheetType = null }
        )
    }
    
    // ==================== PERMISSIONS SHEET ====================
    if (showPermissionsSheet) {
        PermissionsSheet(
            onDismiss = { showPermissionsSheet = false }
        )
    }
}

// ==================== TOP APP BAR ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPipelineTopBar(
    title: String,
    showTaskToggle: Boolean = false,
    taskViewMode: TaskViewMode = TaskViewMode.LIST,
    onTaskViewModeChange: (TaskViewMode) -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
        },
        actions = {
            if (showTaskToggle) {
                IconButton(onClick = {
                    onTaskViewModeChange(
                        if (taskViewMode == TaskViewMode.LIST) TaskViewMode.DATE_WISE else TaskViewMode.LIST
                    )
                }) {
                    Icon(
                        if (taskViewMode == TaskViewMode.LIST) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List,
                        if (taskViewMode == TaskViewMode.LIST) "Calendar View" else "List View",
                        tint = SalesCrmTheme.colors.textSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SalesCrmTheme.colors.background,
            titleContentColor = SalesCrmTheme.colors.textPrimary
        )
    )
}

// ==================== PLACEHOLDER SCREENS ====================

@Composable
fun HomeScreen(
    people: List<Person>,
    tasks: List<Task>,
    activities: List<Activity>,
    onViewTask: (Task) -> Unit,
    onPersonClick: (Int) -> Unit, // Changed to take ID for consistency
    onSync: () -> Unit = {},
    isSyncing: Boolean = false
) {
    val pipelinePeople = people.filter { it.isInPipeline }
    val today = LocalDate.now()
    val todayTasks = tasks.filter { it.dueDate == today && it.status == TaskStatus.PENDING }
    
    val stages = SalesCrmTheme.stages
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header with Sync Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            
            // Sync Button
            Surface(
                onClick = { if (!isSyncing) onSync() },
                shape = CircleShape,
                color = if (isSyncing) SalesCrmTheme.colors.surfaceVariant else PrimaryBlue.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryBlue
                        )
                    } else {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        // Pipeline Stats
        Text(
            "Pipeline Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SalesCrmTheme.colors.textPrimary
        )
        Spacer(Modifier.height(12.dp))
        
        // Stage-wise counts (LazyRow or FlowRow style)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(stages) { stageItem ->
                val count = pipelinePeople.count { it.stageId == stageItem.id }
                StatCard(
                    title = stageItem.label,
                    value = count.toString(),
                    color = Color(stageItem.color),
                    icon = Icons.Default.PieChart
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Task Stats
        Text(
            "Task Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SalesCrmTheme.colors.textPrimary
        )
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val followUpCount = tasks.count { it.type == TaskType.FOLLOW_UP && it.status == TaskStatus.PENDING }
            val meetingCount = tasks.count { it.type == TaskType.MEETING && it.status == TaskStatus.PENDING }
            val todoCount = tasks.count { it.type == TaskType.TO_DO && it.status == TaskStatus.PENDING }
            
            StatCard(
                title = "Follow-ups",
                value = followUpCount.toString(),
                color = AccentOrange,
                icon = Icons.Default.PhoneCallback,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Meetings",
                value = meetingCount.toString(),
                color = PrimaryBlue,
                icon = Icons.Default.Groups,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "To-dos",
                value = todoCount.toString(),
                color = AccentGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Today's Tasks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            if (todayTasks.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = AccentRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${todayTasks.size} Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (todayTasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All caught up for today!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textSecondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                todayTasks.forEach { task ->
                    val person = task.linkedPersonId?.let { id -> people.find { it.id == id } }
                    TodayTaskRow(
                        task = task,
                        personName = person?.name,
                        onClick = { onViewTask(task) },
                        onPersonClick = { person?.let { onPersonClick(it.id) } }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Recent Activities
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Activities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
        }

        Spacer(Modifier.height(12.dp))

        val recentActivities = activities.sortedByDescending { it.timestamp }.take(5)

        if (recentActivities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
            ) {
                Text(
                    "No recent activities",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SalesCrmTheme.colors.textMuted
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentActivities.forEach { activity ->
                    val person = people.find { it.id == activity.personId }
                    RecentActivityRow(
                        activity = activity,
                        personName = person?.name ?: "Unknown",
                        onPersonClick = { person?.let { onPersonClick(it.id) } }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun RecentActivityRow(
    activity: Activity,
    personName: String,
    onPersonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPersonClick() },
        colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (activity.type) {
                            ActivityType.WHATSAPP -> Color(0xFF25D366).copy(alpha = 0.1f)
                            ActivityType.SITE_VISIT -> PrimaryBlue.copy(alpha = 0.1f)
                            ActivityType.DETAILS_SENT -> AccentOrange.copy(alpha = 0.1f)
                            else -> SalesCrmTheme.colors.textMuted.copy(alpha = 0.1f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (activity.type) {
                        ActivityType.WHATSAPP -> Icons.Default.ChatBubbleOutline
                        ActivityType.SITE_VISIT -> Icons.Default.LocationOn
                        ActivityType.DETAILS_SENT -> Icons.AutoMirrored.Filled.Send
                        ActivityType.SYSTEM -> Icons.Default.Settings
                        else -> Icons.Default.Comment
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when (activity.type) {
                        ActivityType.WHATSAPP -> Color(0xFF25D366)
                        ActivityType.SITE_VISIT -> PrimaryBlue
                        ActivityType.DETAILS_SENT -> AccentOrange
                        else -> SalesCrmTheme.colors.textSecondary
                    }
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.titleSmall,
                        color = SalesCrmTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activity.timestamp.toDisplayString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                }
                
                if (activity.title != null) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = SalesCrmTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}    

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(min = 120.dp),
        colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TodayTaskRow(
    task: Task,
    personName: String?,
    onClick: () -> Unit,
    onPersonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (task.type) {
                TaskType.FOLLOW_UP -> Icons.Default.PhoneCallback
                TaskType.MEETING -> Icons.Default.Groups
                TaskType.TO_DO -> Icons.Default.CheckCircle
            }
            val priorityItem = SalesCrmTheme.priorities.findById(task.priorityId)
            val color = Color(priorityItem?.color ?: 0xFF6B7280)
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SalesCrmTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.dueTime?.toDisplayString() ?: "All day",
                        style = MaterialTheme.typography.labelSmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                    if (personName != null) {
                        Text(" • ", color = SalesCrmTheme.colors.textMuted)
                        Text(
                            personName,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onPersonClick() }
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = SalesCrmTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun hasAllPermissions(context: android.content.Context): Boolean {
    val basicPermissions = listOf(
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_CONTACTS
    ).all {
        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    val outgoingPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.PROCESS_OUTGOING_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
    
    val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
    
    val alarmPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true
    
    val overlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.provider.Settings.canDrawOverlays(context)
    } else true

    val batteryPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    return basicPermissions && outgoingPermission && notificationPermission && alarmPermission && overlayPermission && batteryPermission && storagePermission
}

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currencySymbol: String,
    onCurrencyChange: (String) -> Unit,
    defaultCountry: String,
    onDefaultCountryChange: (String) -> Unit,
    budgetMultiplier: Int,
    onBudgetMultiplierChange: (Int) -> Unit,
    customStages: List<CustomItem> = defaultCustomStages,
    customPriorities: List<CustomItem> = defaultCustomPriorities,
    customSegments: List<CustomItem> = defaultCustomSegments,
    customSources: List<CustomItem> = defaultCustomSources,
    onEditStages: () -> Unit = {},
    onEditPriorities: () -> Unit = {},
    onEditSegments: () -> Unit = {},
    onEditSources: () -> Unit = {},
    loggedInPhone: String = "",
    onLogout: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    callerIdEnabled: Boolean = false,
    onCallerIdEnabledChange: (Boolean) -> Unit = {},
    defaultWhatsAppPackage: String = "always_ask",
    onDefaultWhatsAppPackageChange: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val allPermissionsGranted = hasAllPermissions(context)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ==================== PERMISSIONS SECTION ====================
        Text(
            text = "System",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            onClick = onNavigateToPermissions,
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (allPermissionsGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "App Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Text(
                            text = if (allPermissionsGranted) "All permissions granted" else "Some permissions required",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (allPermissionsGranted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allPermissionsGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = SalesCrmTheme.colors.textMuted
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // ==================== FEATURES SECTION ====================
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Caller ID Toggle
                val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) 
                    android.provider.Settings.canDrawOverlays(context) else true
                val hasPhoneStatePermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_PHONE_STATE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val callerIdReady = hasOverlayPermission && hasPhoneStatePermission
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneInTalk,
                                contentDescription = null,
                                tint = if (callerIdEnabled) PrimaryBlue else SalesCrmTheme.colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Caller ID",
                                style = MaterialTheme.typography.titleSmall,
                                color = SalesCrmTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "Show lead info during calls",
                            style = MaterialTheme.typography.bodySmall,
                            color = SalesCrmTheme.colors.textMuted,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                        if (callerIdEnabled && !callerIdReady) {
                            Text(
                                text = "Tap to grant required permissions",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800),
                                modifier = Modifier
                                    .padding(start = 32.dp, top = 4.dp)
                                    .clickable {
                                        // Open to permissions sheet
                                        onNavigateToPermissions()
                                    }
                            )
                        }
                    }
                    
                    Switch(
                        checked = callerIdEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Request permissions when turning ON Caller ID
                                if (!hasOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    val overlayIntent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(overlayIntent)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please allow 'Display over other apps' for Caller ID",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            onCallerIdEnabledChange(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = SalesCrmTheme.colors.textMuted,
                            uncheckedTrackColor = SalesCrmTheme.colors.surfaceVariant
                        )
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // ==================== GENERAL SECTION ====================
        Text(
            text = "General",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Currency Setting
                SettingDropdown(
                    title = "Currency",
                    options = listOf("$", "€", "£", "₹", "¥"),
                    selectedOption = currencySymbol,
                    onOptionSelected = onCurrencyChange
                )
                
                HorizontalDivider(color = SalesCrmTheme.colors.border, modifier = Modifier.padding(vertical = 12.dp))
                
                // Default Country Setting
                SettingDropdown(
                    title = "Default Country",
                    options = listOf("US", "IN", "UK", "CA", "AU"), // IDs
                    displayMap = mapOf("US" to "USA (+1)", "IN" to "India (+91)", "UK" to "UK (+44)", "CA" to "Canada (+1)", "AU" to "Australia (+61)"),
                    selectedOption = defaultCountry,
                    onOptionSelected = onDefaultCountryChange
                )
                
                HorizontalDivider(color = SalesCrmTheme.colors.border, modifier = Modifier.padding(vertical = 12.dp))
                
                // Budget Multiplier Setting
                SettingDropdown(
                    title = "Budget Multiplier",
                    options = listOf("1", "1000", "100000"),
                    displayMap = mapOf(
                        "1" to "None (exact value)",
                        "1000" to "Thousands (K)",
                        "100000" to "Lakhs (L)"
                    ),
                    selectedOption = budgetMultiplier.toString(),
                    onOptionSelected = { onBudgetMultiplierChange(it.toIntOrNull() ?: 1) }
                )
                
                Text(
                    "If set to Lakhs, entering 50 means ₹50L (50,00,000)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SalesCrmTheme.colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HorizontalDivider(color = SalesCrmTheme.colors.border, modifier = Modifier.padding(vertical = 12.dp))

                // WhatsApp Default App Setting
                val whatsappApps = remember(context) { CallLogRepository.getInstalledWhatsAppAppsWithLabels(context) }
                val whatsappOptions = listOf("always_ask") + whatsappApps.map { it.packageName }
                val whatsappDisplayMap = mapOf("always_ask" to "Always Ask") + whatsappApps.associate { it.packageName to it.label }

                SettingDropdown(
                    title = "Default WhatsApp App",
                    options = whatsappOptions,
                    displayMap = whatsappDisplayMap,
                    selectedOption = defaultWhatsAppPackage,
                    onOptionSelected = onDefaultWhatsAppPackageChange
                )
                
                Text(
                    "Choose which WhatsApp app to use for messaging",
                    style = MaterialTheme.typography.labelSmall,
                    color = SalesCrmTheme.colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // ==================== CUSTOMIZATION SECTION ====================
        Text(
            text = "Customization",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Pipeline Stages
                CustomizationRow(
                    title = "Pipeline Stages",
                    description = customStages.joinToString(", ") { it.label },
                    icon = Icons.Default.ViewKanban,
                    onClick = onEditStages
                )
                
                HorizontalDivider(color = SalesCrmTheme.colors.border, modifier = Modifier.padding(vertical = 12.dp))
                
                // Segments
                CustomizationRow(
                    title = "Segments",
                    description = customSegments.joinToString(", ") { it.label },
                    icon = Icons.Default.Category,
                    onClick = onEditSegments
                )
                
                HorizontalDivider(color = SalesCrmTheme.colors.border, modifier = Modifier.padding(vertical = 12.dp))
                
                // Sources
                CustomizationRow(
                    title = "Sources",
                    description = customSources.joinToString(", ") { it.label },
                    icon = Icons.Default.Source,
                    onClick = onEditSources
                )
            }
        }
        
        Text(
            "Configure your own pipeline stages, segments, and sources.",
            style = MaterialTheme.typography.labelSmall,
            color = SalesCrmTheme.colors.textMuted,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        Spacer(Modifier.height(24.dp))
        
        // ==================== APPEARANCE SECTION ====================
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = SalesCrmTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                ThemeOption(
                    title = "System Default",
                    selected = currentThemeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = "Light Mode",
                    selected = currentThemeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = "Dark Mode",
                    selected = currentThemeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // ==================== ACCOUNT SECTION ====================
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            color = SalesCrmTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Logged in as
                if (loggedInPhone.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = SalesCrmTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Logged in as",
                                style = MaterialTheme.typography.labelSmall,
                                color = SalesCrmTheme.colors.textMuted
                            )
                            Text(
                                text = "+91 $loggedInPhone",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SalesCrmTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = SalesCrmTheme.colors.border, 
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                // Logout Button
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset All Data Button
                var showResetConfirm by remember { mutableStateOf(false) }
                
                TextButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SalesCrmTheme.colors.textMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Account (Wipe all data)",
                        style = MaterialTheme.typography.labelMedium,
                        textDecoration = TextDecoration.Underline
                    )
                }

                if (showResetConfirm) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirm = false },
                        title = { Text("Reset Account?") },
                        text = { 
                            Text("This will permanently delete all your leads, tasks, and activities from both this device and the cloud. This action cannot be undone.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showResetConfirm = false
                                    onResetAllData()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                            ) {
                                Text("Reset Everything")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirm = false }) {
                                Text("Cancel")
                            }
                        },
                        containerColor = SalesCrmTheme.colors.surface,
                        titleContentColor = SalesCrmTheme.colors.textPrimary,
                        textContentColor = SalesCrmTheme.colors.textSecondary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val multiplePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTrigger++ }

    val singlePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val permissionsList = remember(refreshTrigger) {
        val list = mutableListOf<PermissionItemData>()
        
        // 1. Notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            list.add(PermissionItemData(
                title = "Notifications",
                purpose = "Show task reminders and call alerts.",
                isGranted = isGranted,
                action = {
                    if (!isGranted) {
                        singlePermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
            ))
        }

        // 2. Call Tracking
        val hasCallLog = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPhoneState = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasProcessOutgoing = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.PROCESS_OUTGOING_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        
        val callTrackingGranted = hasCallLog && hasPhoneState && hasProcessOutgoing
        list.add(PermissionItemData(
            title = "Call Tracking",
            purpose = "Detect incoming/outgoing calls to identify leads.",
            isGranted = callTrackingGranted,
            action = {
                if (!callTrackingGranted) {
                    val perms = mutableListOf(
                        android.Manifest.permission.READ_CALL_LOG,
                        android.Manifest.permission.READ_PHONE_STATE
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        perms.add(android.Manifest.permission.PROCESS_OUTGOING_CALLS)
                    }
                    multiplePermissionLauncher.launch(perms.toTypedArray())
                } else {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        ))

        // 3. Contacts
        val hasContacts = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        list.add(PermissionItemData(
            title = "Contacts",
            purpose = "Link CRM leads with your device contacts.",
            isGranted = hasContacts,
            action = {
                if (!hasContacts) {
                    singlePermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                } else {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        ))

        // 4. Exact Alarms
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            list.add(PermissionItemData(
                title = "Exact Alarms",
                purpose = "Required for precise reminder delivery.",
                isGranted = alarmManager.canScheduleExactAlarms(),
                action = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            ))
        }

        // 5. Overlay
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            list.add(PermissionItemData(
                title = "Display over other apps",
                purpose = "Show Caller ID banner during active calls.",
                isGranted = android.provider.Settings.canDrawOverlays(context),
                action = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            ))
        }
        
        // 6. Battery Optimization
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            list.add(PermissionItemData(
                title = "Battery Optimization",
                purpose = "Allow Caller ID and call monitoring to work when app is in background or closed.",
                isGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName),
                action = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            ))
        }

        // 7. Storage (for recordings)
        val hasStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        list.add(PermissionItemData(
            title = "Storage",
            purpose = "Required to play and manage call recordings.",
            isGranted = hasStorage,
            action = {
                if (!hasStorage) {
                    val perm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    singlePermissionLauncher.launch(perm)
                } else {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        ))
        
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.85f)
        ) {
            // Header with Title and Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "App Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                IconButton(onClick = { refreshTrigger++ }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = PrimaryBlue
                    )
                }
            }
            
            Text(
                text = "Please enable the following permissions to ensure all app features work correctly.",
                style = MaterialTheme.typography.bodyMedium,
                color = SalesCrmTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(permissionsList) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SalesCrmTheme.colors.background
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SalesCrmTheme.colors.textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.purpose,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SalesCrmTheme.colors.textSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (item.isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (item.isGranted) "Granted" else "Action Required",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                            
                            Button(
                                onClick = item.action,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (item.isGranted) SalesCrmTheme.colors.border else PrimaryBlue,
                                    contentColor = if (item.isGranted) SalesCrmTheme.colors.textPrimary else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (item.isGranted) "Modify" else "Grant",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PermissionItemData(
    val title: String,
    val purpose: String,
    val isGranted: Boolean,
    val action: () -> Unit
)

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF10B981) else SalesCrmTheme.colors.textMuted,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SalesCrmTheme.colors.textMuted
            )
        }
        
        if (isGranted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
        } else {
            TextButton(onClick = onRequest) {
                Text("Grant", color = PrimaryBlue)
            }
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = PrimaryBlue,
                unselectedColor = SalesCrmTheme.colors.textMuted
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = SalesCrmTheme.colors.textPrimary,
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
fun SettingDropdown(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    displayMap: Map<String, String> = emptyMap()
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = SalesCrmTheme.colors.textPrimary
        )
        
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = SalesCrmTheme.colors.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayMap[selectedOption] ?: selectedOption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        tint = SalesCrmTheme.colors.textSecondary
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SalesCrmTheme.colors.surface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                displayMap[option] ?: option, 
                                color = SalesCrmTheme.colors.textPrimary
                            ) 
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomizationRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SalesCrmTheme.colors.textMuted,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SalesCrmTheme.colors.textMuted,
                maxLines = 1
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SalesCrmTheme.colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}