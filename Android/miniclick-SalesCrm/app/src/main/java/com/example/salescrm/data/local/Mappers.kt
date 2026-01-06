package com.example.salescrm.data.local

import com.example.salescrm.data.*

fun PersonEntity.toDomain(): Person = Person(
    id = id,
    name = name,
    phone = phone,
    alternativePhone = alternativePhone,
    address = address,
    about = about,
    note = note,
    labels = labels,
    segmentId = segmentId,
    sourceId = sourceId,
    budget = budget,
    priorityId = priorityId,
    isInPipeline = isInPipeline,
    stageId = stageId,
    pipelinePriorityId = pipelinePriorityId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastOpenedAt = lastOpenedAt,
    lastActivityAt = lastActivityAt,
    activityCount = activityCount
)

fun Person.toEntity(): PersonEntity = PersonEntity(
    id = if (id == 0) 0 else id, // Handle auto-gen for new
    name = name,
    phone = phone,
    alternativePhone = alternativePhone,
    address = address,
    about = about,
    note = note,
    labels = labels,
    segmentId = segmentId,
    sourceId = sourceId,
    budget = budget,
    priorityId = priorityId,
    isInPipeline = isInPipeline,
    stageId = stageId,
    pipelinePriorityId = pipelinePriorityId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = null, // Reset sync status on manual edit
    lastOpenedAt = lastOpenedAt,
    lastActivityAt = lastActivityAt,
    activityCount = activityCount
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    linkedPersonId = linkedPersonId,
    type = type,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priorityId = priorityId,
    status = status,
    showReminder = showReminder,
    reminderMinutesBefore = reminderMinutesBefore,
    response = response,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    linkedPersonId = linkedPersonId,
    type = type,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    priorityId = priorityId,
    status = status,
    showReminder = showReminder,
    reminderMinutesBefore = reminderMinutesBefore,
    response = response,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = null,
    completedAt = completedAt
)

fun ActivityEntity.toDomain(): Activity = Activity(
    id = id,
    personId = personId,
    type = ActivityType.valueOf(type),
    title = title,
    description = description,
    timestamp = timestamp,
    updatedAt = updatedAt,
    recordingPath = recordingPath,
    metadata = metadata
)

fun Activity.toEntity(): ActivityEntity = ActivityEntity(
    id = id,
    personId = personId,
    type = type.name,
    title = title,
    description = description,
    timestamp = timestamp,
    updatedAt = updatedAt,
    lastSyncedAt = null,
    recordingPath = recordingPath,
    metadata = metadata
)

fun CustomItemEntity.toDomain(): CustomItem = CustomItem(
    id = id,
    label = label,
    color = color,
    order = order
)

fun CustomItem.toEntity(type: String): CustomItemEntity = CustomItemEntity(
    id = id,
    type = type,
    label = label,
    color = color,
    order = order
)

fun CallHistoryEntity.toDomain(): CallLogEntry = CallLogEntry(
    id = id,
    phoneNumber = phoneNumber,
    normalizedNumber = normalizedNumber,
    contactName = contactName,
    callType = CallType.valueOf(callType),
    duration = duration,
    timestamp = timestamp,
    isNew = isNew,
    recordingPath = recordingPath,
    linkedPersonId = linkedPersonId,
    note = note,
    subscriptionId = subscriptionId
)

fun CallLogEntry.toEntity(): CallHistoryEntity = CallHistoryEntity(
    id = id,
    phoneNumber = phoneNumber,
    normalizedNumber = normalizedNumber,
    contactName = contactName,
    callType = callType.name,
    duration = duration,
    timestamp = timestamp,
    isNew = isNew,
    recordingPath = recordingPath,
    linkedPersonId = linkedPersonId,
    note = note,
    subscriptionId = subscriptionId
)
