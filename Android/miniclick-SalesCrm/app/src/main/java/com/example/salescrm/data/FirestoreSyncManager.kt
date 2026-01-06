package com.example.salescrm.data

import android.util.Log
import com.example.salescrm.data.local.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

class FirestoreSyncManager(
    private val context: android.content.Context, 
    private val salesDao: SalesDao,
    private val userPrefs: UserPreferencesRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    
    // Safety flag to prevent local Room updates (from cloud pull) from triggering a re-upload
    private var isPullingFromCloud = false
    
    // Track listeners for proper cleanup
    private val listenerRegistrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun startSync(phone: String) {
        stopSync()
        syncJob = scope.launch {
            // Phase 1: Initial incremental pull (one-time query, NOT listener)
            // This only reads docs changed since last sync - HUGE cost savings
            val lastSyncTime = userPrefs.getLastCloudSyncTimestampOnce()
            Log.d("FirestoreSync", "Starting sync. Last sync: $lastSyncTime")
            
            // Pull only changed data since last sync (or everything if first time)
            pullIncrementalChanges(phone, lastSyncTime)
            
            // Save the current timestamp for next sync
            userPrefs.saveLastCloudSyncTimestamp(LocalDateTime.now().toString())
            
            // Phase 2: PUSH (Local -> Cloud) - only syncs changed docs
            launch { observeAndPushPeople(phone) }
            launch { observeAndPushTasks(phone) }
            launch { observeAndPushActivities(phone) }
            launch { observeAndPushCustomItems(phone) }
            
            // Phase 3: Lightweight real-time listeners for new changes while app is open
            // These listeners are now VERY cheap since we already have all the data
            launch { listenForNewChanges(phone) }
        }
    }

    fun stopSync() {
        // Clean up all Firestore listeners
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        
        syncJob?.cancel()
        syncJob = null
    }
    
    // Force a full re-sync of all data (ignores lastSyncTimestamp)
    fun forceFullSync(phone: String, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                stopSync()
                
                // Reset the sync timestamp so we pull everything
                userPrefs.saveLastCloudSyncTimestamp(null)
                
                // Pull all data from cloud
                pullIncrementalChanges(phone, null)
                
                // Pull config settings
                pullConfigFromCloud(phone)
                
                // Save the current timestamp for next sync
                userPrefs.saveLastCloudSyncTimestamp(LocalDateTime.now().toString())
                
                // Restart regular sync
                startSync(phone)
                
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("FirestoreSync", "Force full sync failed", e)
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }
    
    // --- CONFIG SYNC (User Settings) ---
    
    suspend fun pushConfigToCloud(
        phone: String,
        themeMode: String,
        currencySymbol: String,
        defaultCountry: String,
        budgetMultiplier: Int,
        taskViewMode: String
    ) {
        try {
            val configMap = mapOf(
                "themeMode" to themeMode,
                "currencySymbol" to currencySymbol,
                "defaultCountry" to defaultCountry,
                "budgetMultiplier" to budgetMultiplier,
                "taskViewMode" to taskViewMode,
                "updatedAt" to LocalDateTime.now().toString()
            )
            firestore.collection("users").document(phone)
                .collection("settings").document("config")
                .set(configMap, SetOptions.merge()).await()
            Log.d("FirestoreSync", "Pushed config to cloud")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Push config failed", e)
        }
    }
    
    private suspend fun pullConfigFromCloud(phone: String) {
        try {
            val configDoc = firestore.collection("users").document(phone)
                .collection("settings").document("config")
                .get().await()
            
            if (configDoc.exists()) {
                val data = configDoc.data ?: return
                
                // Save to local preferences
                (data["themeMode"] as? String)?.let { 
                    userPrefs.saveThemeMode(ThemeMode.valueOf(it)) 
                }
                (data["currencySymbol"] as? String)?.let { 
                    userPrefs.saveCurrency(it) 
                }
                (data["defaultCountry"] as? String)?.let { 
                    userPrefs.saveDefaultCountry(it) 
                }
                (data["budgetMultiplier"] as? Number)?.toInt()?.let { 
                    userPrefs.saveBudgetMultiplier(it) 
                }
                (data["taskViewMode"] as? String)?.let { 
                    userPrefs.saveTaskViewMode(TaskViewMode.valueOf(it)) 
                }
                
                Log.d("FirestoreSync", "Pulled config from cloud")
            }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Pull config failed", e)
        }
    }
    
    // --- INCREMENTAL PULL (Cost-efficient: Only reads changed docs) ---
    
    private suspend fun pullIncrementalChanges(phone: String, lastSyncTimestamp: String?) {
        val userRef = firestore.collection("users").document(phone)
        
        try {
            // Pull People
            val peopleQuery = if (lastSyncTimestamp != null) {
                userRef.collection("people").whereGreaterThan("updatedAt", lastSyncTimestamp)
            } else {
                userRef.collection("people") // First time: get all
            }
            
            val peopleSnapshot = peopleQuery.get().await()
            isPullingFromCloud = true
            peopleSnapshot.documents.forEach { doc ->
                val id = doc.id.toIntOrNull() ?: return@forEach
                val cloudEntity = doc.data?.toPersonEntity(id) ?: return@forEach
                salesDao.insertPerson(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
                Log.d("FirestoreSync", "Incremental pull: Person $id")
            }
            Log.d("FirestoreSync", "Pulled ${peopleSnapshot.size()} people (changed since $lastSyncTimestamp)")
            
            // Pull Tasks
            val tasksQuery = if (lastSyncTimestamp != null) {
                userRef.collection("tasks").whereGreaterThan("updatedAt", lastSyncTimestamp)
            } else {
                userRef.collection("tasks")
            }
            
            val tasksSnapshot = tasksQuery.get().await()
            tasksSnapshot.documents.forEach { doc ->
                val id = doc.id.toIntOrNull() ?: return@forEach
                val cloudEntity = doc.data?.toTaskEntity(id) ?: return@forEach
                salesDao.insertTask(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
                Log.d("FirestoreSync", "Incremental pull: Task $id")
            }
            Log.d("FirestoreSync", "Pulled ${tasksSnapshot.size()} tasks")
            
            // Pull Activities
            val activitiesQuery = if (lastSyncTimestamp != null) {
                userRef.collection("activities").whereGreaterThan("updatedAt", lastSyncTimestamp)
            } else {
                userRef.collection("activities")
            }
            
            val activitiesSnapshot = activitiesQuery.get().await()
            activitiesSnapshot.documents.forEach { doc ->
                val id = doc.id.toIntOrNull() ?: return@forEach
                val cloudEntity = doc.data?.toActivityEntity(id) ?: return@forEach
                salesDao.insertActivity(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
            }
            Log.d("FirestoreSync", "Pulled ${activitiesSnapshot.size()} activities")
            
            // Pull CustomItems (always pull all - they're small and rarely change)
            // For full sync (lastSyncTimestamp == null), replace local with cloud
            val customItemsSnapshot = userRef.collection("customItems").get().await()
            val cloudItems = mutableListOf<CustomItemEntity>()
            customItemsSnapshot.documents.forEach { doc ->
                val id = doc.id
                val type = id.substringBefore("_")
                val itemId = id.substringAfter("_")
                doc.data?.let { data ->
                    cloudItems.add(data.toCustomItemEntity(itemId, type))
                }
            }
            
            // If this is a full sync, replace all custom items with cloud data
            if (lastSyncTimestamp == null && cloudItems.isNotEmpty()) {
                // Delete all local custom items first
                val localItems = salesDao.getAllCustomItemsOnce()
                localItems.forEach { salesDao.deleteCustomItemByIdAndType(it.id, it.type) }
                // Insert cloud items
                salesDao.insertCustomItems(cloudItems)
                Log.d("FirestoreSync", "Replaced local custom items with ${cloudItems.size} from cloud")
            } else {
                // Incremental - just insert/update
                salesDao.insertCustomItems(cloudItems)
                Log.d("FirestoreSync", "Pulled ${customItemsSnapshot.size()} custom items")
            }
            
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Incremental pull failed", e)
        } finally {
            delay(500)
            isPullingFromCloud = false
        }
    }
    
    // --- LIGHTWEIGHT REAL-TIME LISTENERS (Only for changes while app is open) ---
    
    private fun listenForNewChanges(phone: String) {
        val userRef = firestore.collection("users").document(phone)
        val now = LocalDateTime.now().toString()
        
        // Only listen for changes happening NOW (not replaying old data)
        // This is nearly FREE - we only pay for actual new changes
        
        val peopleListener = userRef.collection("people")
            .whereGreaterThan("updatedAt", now)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                handlePeopleChanges(snapshots)
            }
        listenerRegistrations.add(peopleListener)
        
        val tasksListener = userRef.collection("tasks")
            .whereGreaterThan("updatedAt", now)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                handleTaskChanges(snapshots)
            }
        listenerRegistrations.add(tasksListener)
        
        val activitiesListener = userRef.collection("activities")
            .whereGreaterThan("updatedAt", now)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                handleActivityChanges(snapshots)
            }
        listenerRegistrations.add(activitiesListener)
        
        val customItemsListener = userRef.collection("customItems")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                handleCustomItemChanges(snapshots)
            }
        listenerRegistrations.add(customItemsListener)
        
        Log.d("FirestoreSync", "Real-time listeners attached for changes after $now")
    }
    
    private fun handlePeopleChanges(snapshots: com.google.firebase.firestore.QuerySnapshot?) {
        if (snapshots == null) return
        isPullingFromCloud = true
        scope.launch {
            try {
                snapshots.documentChanges.forEach { dc ->
                    val id = dc.document.id.toIntOrNull() ?: return@forEach
                    val data = dc.document.data
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val cloudEntity = data.toPersonEntity(id)
                            salesDao.insertPerson(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
                            Log.d("FirestoreSync", "Real-time: Person $id updated")
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            salesDao.deletePerson(PersonEntity(id = id, name = "", phone = ""))
                        }
                    }
                }
            } finally {
                delay(500)
                isPullingFromCloud = false
            }
        }
    }
    
    private fun handleTaskChanges(snapshots: com.google.firebase.firestore.QuerySnapshot?) {
        if (snapshots == null) return
        isPullingFromCloud = true
        scope.launch {
            try {
                snapshots.documentChanges.forEach { dc ->
                    val id = dc.document.id.toIntOrNull() ?: return@forEach
                    val data = dc.document.data
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val cloudEntity = data.toTaskEntity(id)
                            salesDao.insertTask(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
                            Log.d("FirestoreSync", "Real-time: Task $id updated")
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            salesDao.deleteTask(TaskEntity(id = id, linkedPersonId = null, type = TaskType.TO_DO, dueDate = LocalDate.now()))
                        }
                    }
                }
            } finally {
                delay(500)
                isPullingFromCloud = false
            }
        }
    }
    
    private fun handleActivityChanges(snapshots: com.google.firebase.firestore.QuerySnapshot?) {
        if (snapshots == null) return
        isPullingFromCloud = true
        scope.launch {
            try {
                snapshots.documentChanges.forEach { dc ->
                    val id = dc.document.id.toIntOrNull() ?: return@forEach
                    val data = dc.document.data
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val cloudEntity = data.toActivityEntity(id)
                            salesDao.insertActivity(cloudEntity.copy(lastSyncedAt = cloudEntity.updatedAt))
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            salesDao.deleteActivity(ActivityEntity(id = id, personId = 0, type = ActivityType.SYSTEM.name, description = "", timestamp = LocalDateTime.now()))
                        }
                    }
                }
            } finally {
                delay(500)
                isPullingFromCloud = false
            }
        }
    }
    
    private fun handleCustomItemChanges(snapshots: com.google.firebase.firestore.QuerySnapshot?) {
        if (snapshots == null) return
        isPullingFromCloud = true
        scope.launch {
            try {
                snapshots.documentChanges.forEach { dc ->
                    val id = dc.document.id
                    val type = id.substringBefore("_")
                    val itemId = id.substringAfter("_")
                    val data = dc.document.data
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            salesDao.insertCustomItems(listOf(data.toCustomItemEntity(itemId, type)))
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            salesDao.deleteCustomItemByIdAndType(itemId, type)
                        }
                    }
                }
            } finally {
                delay(500)
                isPullingFromCloud = false
            }
        }
    }

    // --- CLOUD CLEANUP METHODS ---

    fun deletePersonCloud(phone: String, personId: Int) {
        val userRef = firestore.collection("users").document(phone)
        userRef.collection("people").document(personId.toString()).delete()
        
        // Manual cascade for Firestore
        scope.launch {
            val tasks = userRef.collection("tasks").whereEqualTo("linkedPersonId", personId).get().await()
            tasks.forEach { it.reference.delete() }
            val activities = userRef.collection("activities").whereEqualTo("personId", personId).get().await()
            activities.forEach { it.reference.delete() }
        }
    }

    fun deleteTaskCloud(phone: String, taskId: Int) {
        firestore.collection("users").document(phone)
            .collection("tasks").document(taskId.toString()).delete()
    }

    fun deleteActivityCloud(phone: String, activityId: Int) {
        firestore.collection("users").document(phone)
            .collection("activities").document(activityId.toString()).delete()
    }

    suspend fun clearCloudData(phone: String) {
        val userRef = firestore.collection("users").document(phone)
        val collections = listOf("people", "tasks", "activities", "customItems")
        collections.forEach { col ->
            val snapshot = firestore.collection("users").document(phone).collection(col).get().await()
            snapshot.documents.forEach { it.reference.delete() }
        }
    }

    // --- PUSH LOGIC (Local -> Firestore) ---

    private suspend fun observeAndPushPeople(phone: String) {
        // 500ms debounce - just to batch any rapid consecutive saves
        salesDao.getAllPeople()
            .debounce(500)
            .collect { entities ->
                if (isPullingFromCloud) return@collect
                
                // Early exit: Check if ANY entity needs syncing before touching Firestore
                val entitiesToSync = entities.filter { entity ->
                    entity.lastSyncedAt == null || entity.updatedAt.isAfter(entity.lastSyncedAt)
                }
                if (entitiesToSync.isEmpty()) return@collect
                
                val peopleCol = firestore.collection("users").document(phone).collection("people")
                
                entitiesToSync.forEach { entity ->
                    try {
                        peopleCol.document(entity.id.toString()).set(entity.toMap(), SetOptions.merge()).await()
                        salesDao.updatePersonSyncTimestamp(entity.id, entity.updatedAt)
                        Log.d("FirestoreSync", "Pushed person ${entity.id}")
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Push Person failed", e)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Sync Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private suspend fun observeAndPushTasks(phone: String) {
        salesDao.getAllTasks()
            .debounce(500)
            .collect { entities ->
                if (isPullingFromCloud) return@collect
                
                val entitiesToSync = entities.filter { entity ->
                    entity.lastSyncedAt == null || entity.updatedAt.isAfter(entity.lastSyncedAt)
                }
                if (entitiesToSync.isEmpty()) return@collect
                
                val tasksCol = firestore.collection("users").document(phone).collection("tasks")
                
                entitiesToSync.forEach { entity ->
                    try {
                        tasksCol.document(entity.id.toString()).set(entity.toMap(), SetOptions.merge()).await()
                        salesDao.updateTaskSyncTimestamp(entity.id, entity.updatedAt)
                        Log.d("FirestoreSync", "Pushed task ${entity.id}")
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Push Task failed", e)
                    }
                }
            }
    }

    private suspend fun observeAndPushActivities(phone: String) {
        salesDao.getAllActivities()
            .debounce(500)
            .collect { entities ->
                if (isPullingFromCloud) return@collect
                
                val entitiesToSync = entities.filter { entity ->
                    entity.lastSyncedAt == null || entity.updatedAt.isAfter(entity.lastSyncedAt)
                }
                if (entitiesToSync.isEmpty()) return@collect
                
                val activitiesCol = firestore.collection("users").document(phone).collection("activities")
                
                entitiesToSync.forEach { entity ->
                    try {
                        activitiesCol.document(entity.id.toString()).set(entity.toMap(), SetOptions.merge()).await()
                        salesDao.updateActivitySyncTimestamp(entity.id, entity.updatedAt)
                        Log.d("FirestoreSync", "Pushed activity ${entity.id}")
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Push Activity failed", e)
                    }
                }
            }
    }

    private suspend fun observeAndPushCustomItems(phone: String) {
        // Custom items rarely change, 2s debounce is fine
        salesDao.getAllCustomItems()
            .debounce(2000)
            .collect { entities ->
                if (isPullingFromCloud) return@collect
                
                val customItemsCol = firestore.collection("users").document(phone).collection("customItems")
                
                try {
                    // Get all existing cloud custom items
                    val cloudSnapshot = customItemsCol.get().await()
                    val cloudDocIds = cloudSnapshot.documents.map { it.id }.toSet()
                    
                    // Get local item IDs
                    val localDocIds = entities.map { "${it.type}_${it.id}" }.toSet()
                    
                    // Delete items from cloud that no longer exist locally
                    val toDelete = cloudDocIds - localDocIds
                    toDelete.forEach { docId ->
                        customItemsCol.document(docId).delete().await()
                        Log.d("FirestoreSync", "Deleted custom item from cloud: $docId")
                    }
                    
                    // Push/update local items to cloud
                    entities.forEach { entity ->
                        val docId = "${entity.type}_${entity.id}"
                        customItemsCol.document(docId).set(entity.toMap(), SetOptions.merge()).await()
                    }
                    
                    Log.d("FirestoreSync", "Synced ${entities.size} custom items, deleted ${toDelete.size}")
                } catch (e: Exception) {
                    Log.e("FirestoreSync", "Push CustomItems failed", e)
                }
            }
    }

    // --- MAPPING HELPERS (Map -> Entity) ---

    private fun Map<String, Any?>.toPersonEntity(id: Int): PersonEntity {
        return PersonEntity(
            id = id,
            name = this["name"] as? String ?: "",
            phone = this["phone"] as? String ?: "",
            alternativePhone = this["alternativePhone"] as? String ?: "",
            address = this["address"] as? String ?: "",
            about = this["about"] as? String ?: "",
            note = this["note"] as? String ?: "",
            labels = (this["labels"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            segmentId = this["segmentId"] as? String ?: "new",
            sourceId = this["sourceId"] as? String ?: "other",
            budget = this["budget"] as? String ?: "",
            priorityId = this["priorityId"] as? String ?: "none",
            isInPipeline = this["isInPipeline"] as? Boolean ?: false,
            stageId = this["stageId"] as? String ?: "fresh",
            pipelinePriorityId = this["pipelinePriorityId"] as? String ?: "medium",
            createdAt = LocalDateTime.parse(this["createdAt"] as? String ?: LocalDateTime.now().toString()),
            updatedAt = LocalDateTime.parse(this["updatedAt"] as? String ?: LocalDateTime.now().toString()),
            lastOpenedAt = (this["lastOpenedAt"] as? String)?.let { LocalDateTime.parse(it) },
            lastActivityAt = (this["lastActivityAt"] as? String)?.let { LocalDateTime.parse(it) },
            activityCount = (this["activityCount"] as? Number)?.toInt() ?: 0
        )
    }

    private fun Map<String, Any?>.toTaskEntity(id: Int): TaskEntity {
        return TaskEntity(
            id = id,
            linkedPersonId = (this["linkedPersonId"] as? Number)?.toInt(),
            type = TaskType.valueOf(this["type"] as? String ?: TaskType.TO_DO.name),
            description = this["description"] as? String ?: "",
            dueDate = LocalDate.parse(this["dueDate"] as? String ?: LocalDate.now().toString()),
            dueTime = (this["dueTime"] as? String)?.let { LocalTime.parse(it) },
            priorityId = this["priorityId"] as? String ?: "medium",
            status = TaskStatus.valueOf(this["status"] as? String ?: TaskStatus.PENDING.name),
            showReminder = this["showReminder"] as? Boolean ?: false,
            reminderMinutesBefore = (this["reminderMinutesBefore"] as? Number)?.toInt() ?: 30,
            response = this["response"] as? String ?: "",
            createdAt = LocalDateTime.parse(this["createdAt"] as? String ?: LocalDateTime.now().toString()),
            updatedAt = LocalDateTime.parse(this["updatedAt"] as? String ?: LocalDateTime.now().toString()),
            completedAt = (this["completedAt"] as? String)?.let { LocalDateTime.parse(it) }
        )
    }

    private fun Map<String, Any?>.toActivityEntity(id: Int): ActivityEntity {
        return ActivityEntity(
            id = id,
            personId = (this["personId"] as? Number)?.toInt() ?: 0,
            type = this["type"] as? String ?: ActivityType.SYSTEM.name,
            title = this["title"] as? String,
            description = this["description"] as? String ?: "",
            timestamp = LocalDateTime.parse(this["timestamp"] as? String ?: LocalDateTime.now().toString()),
            updatedAt = LocalDateTime.parse(this["updatedAt"] as? String ?: LocalDateTime.now().toString()),
            recordingPath = this["recordingPath"] as? String,
            metadata = (this["metadata"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyList<Pair<String,String>>().toMap()
        )
    }

    private fun Map<String, Any?>.toCustomItemEntity(id: String, type: String): CustomItemEntity {
        return CustomItemEntity(
            id = id,
            type = type,
            label = this["label"] as? String ?: "",
            color = (this["color"] as? Number)?.toLong() ?: 0L,
            order = (this["order"] as? Number)?.toInt() ?: 0
        )
    }

    // --- MAPPING HELPERS (Entity -> Map) ---

    private fun PersonEntity.toMap() = mapOf(
        "name" to name, "phone" to phone, "alternativePhone" to alternativePhone,
        "address" to address, "about" to about, "note" to note, "labels" to labels,
        "segmentId" to segmentId, "sourceId" to sourceId, "budget" to budget,
        "priorityId" to priorityId, "isInPipeline" to isInPipeline, "stageId" to stageId,
        "pipelinePriorityId" to pipelinePriorityId, "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(), "lastOpenedAt" to lastOpenedAt?.toString(),
        "lastActivityAt" to lastActivityAt?.toString(), "activityCount" to activityCount
    )

    private fun TaskEntity.toMap() = mapOf(
        "linkedPersonId" to linkedPersonId, "type" to type.name, "description" to description,
        "dueDate" to dueDate.toString(), "dueTime" to dueTime?.toString(),
        "priorityId" to priorityId, "status" to status.name, "showReminder" to showReminder,
        "reminderMinutesBefore" to reminderMinutesBefore, "response" to response,
        "createdAt" to createdAt.toString(), "updatedAt" to updatedAt.toString(),
        "completedAt" to completedAt?.toString()
    )

    private fun ActivityEntity.toMap() = mapOf(
        "personId" to personId, "type" to type, "title" to title, "description" to description,
        "timestamp" to timestamp.toString(), "updatedAt" to updatedAt.toString(),
        "recordingPath" to recordingPath, "metadata" to metadata
    )

    private fun CustomItemEntity.toMap() = mapOf(
        "label" to label, "color" to color, "order" to order
    )
}
