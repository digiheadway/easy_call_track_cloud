package com.example.smsblaster.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsblaster.data.AppDatabase
import com.example.smsblaster.data.model.*
import com.example.smsblaster.sms.SmsResult
import com.example.smsblaster.sms.SmsService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CampaignWithTemplate(
    val campaign: Campaign,
    val template: Template?
)

class CampaignViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CampaignViewModel"
    }
    
    private val db = AppDatabase.getDatabase(application)
    private val campaignDao = db.campaignDao()
    private val campaignMessageDao = db.campaignMessageDao()
    private val templateDao = db.templateDao()
    private val contactDao = db.contactDao()
    private val smsService = SmsService(application)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val allCampaigns = campaignDao.getAllCampaigns()
    private val allTemplates = templateDao.getAllTemplates()
    
    val campaignsWithTemplates: StateFlow<List<CampaignWithTemplate>> = combine(
        allCampaigns,
        allTemplates,
        _searchQuery
    ) { campaigns, templates, query ->
        val templateMap = templates.associateBy { it.id }
        campaigns
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { campaign ->
                CampaignWithTemplate(
                    campaign = campaign,
                    template = campaign.templateId?.let { templateMap[it] }
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val campaignCount: StateFlow<Int> = campaignDao.getCampaignCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    private val _currentCampaign = MutableStateFlow<Campaign?>(null)
    val currentCampaign: StateFlow<Campaign?> = _currentCampaign.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private var currentSimSubscriptionId: Int? = null
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun createCampaign(
        name: String,
        templateId: Long,
        recipientIds: List<Long>,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Creating campaign: $name with ${recipientIds.size} recipients")
            
            val campaign = Campaign(
                name = name,
                templateId = templateId,
                recipientIds = recipientIds,
                totalCount = recipientIds.size,
                status = CampaignStatus.DRAFT
            )
            val campaignId = campaignDao.insertCampaign(campaign)
            Log.d(TAG, "Campaign created with ID: $campaignId")
            
            // Create campaign messages
            val template = templateDao.getTemplateById(templateId)
            val contacts = contactDao.getContactsByIds(recipientIds)
            
            Log.d(TAG, "Template: ${template?.name}, Contacts: ${contacts.size}")
            
            if (template != null) {
                val messages = contacts.map { contact ->
                    val formattedMessage = template.formatMessage(contact)
                    Log.d(TAG, "Message for ${contact.name} (${contact.phone}): $formattedMessage")
                    CampaignMessage(
                        campaignId = campaignId,
                        contactId = contact.id,
                        phoneNumber = contact.phone,
                        message = formattedMessage
                    )
                }
                campaignMessageDao.insertMessages(messages)
                Log.d(TAG, "Inserted ${messages.size} campaign messages")
            }
            
            onComplete(campaignId)
        }
    }
    
    fun duplicateCampaign(campaignId: Long, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "Duplicating campaign: $campaignId")
            
            val originalCampaign = campaignDao.getCampaignById(campaignId)
            if (originalCampaign == null) {
                Log.e(TAG, "Original campaign not found")
                return@launch
            }
            
            val newCampaign = originalCampaign.copy(
                id = 0,
                name = "${originalCampaign.name} (Copy)",
                status = CampaignStatus.DRAFT,
                sentCount = 0,
                failedCount = 0,
                startedAt = null,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val newCampaignId = campaignDao.insertCampaign(newCampaign)
            
            // Duplicate messages
            val template = originalCampaign.templateId?.let { templateDao.getTemplateById(it) }
            val contacts = contactDao.getContactsByIds(originalCampaign.recipientIds)
            
            if (template != null) {
                val messages = contacts.map { contact ->
                    CampaignMessage(
                        campaignId = newCampaignId,
                        contactId = contact.id,
                        phoneNumber = contact.phone,
                        message = template.formatMessage(contact)
                    )
                }
                campaignMessageDao.insertMessages(messages)
            }
            
            Log.d(TAG, "Campaign duplicated with ID: $newCampaignId")
            onComplete(newCampaignId)
        }
    }
    
    fun updateCampaign(campaign: Campaign) {
        viewModelScope.launch {
            campaignDao.updateCampaign(campaign.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    fun deleteCampaign(campaign: Campaign) {
        viewModelScope.launch {
            campaignDao.deleteCampaign(campaign)
        }
    }
    
    fun updateCampaignDetails(
        campaignId: Long,
        templateId: Long,
        recipientIds: List<Long>
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Updating campaign details: $campaignId, templateId=$templateId, recipients=${recipientIds.size}")
            
            val campaign = campaignDao.getCampaignById(campaignId) ?: return@launch
            
            // Delete old messages
            campaignMessageDao.deleteMessagesByCampaignId(campaignId)
            
            // Update campaign
            val updatedCampaign = campaign.copy(
                templateId = templateId,
                recipientIds = recipientIds,
                totalCount = recipientIds.size,
                sentCount = 0,
                failedCount = 0,
                status = CampaignStatus.DRAFT,
                updatedAt = System.currentTimeMillis()
            )
            campaignDao.updateCampaign(updatedCampaign)
            
            // Create new messages
            val template = templateDao.getTemplateById(templateId)
            val contacts = contactDao.getContactsByIds(recipientIds)
            
            if (template != null) {
                val messages = contacts.map { contact ->
                    Log.d(TAG, "Creating message for ${contact.name}: customKeys=${contact.customKeys}")
                    val formattedMessage = template.formatMessage(contact)
                    Log.d(TAG, "Formatted message: $formattedMessage")
                    CampaignMessage(
                        campaignId = campaignId,
                        contactId = contact.id,
                        phoneNumber = contact.phone,
                        message = formattedMessage
                    )
                }
                campaignMessageDao.insertMessages(messages)
                Log.d(TAG, "Created ${messages.size} new messages")
            }
        }
    }
    
    suspend fun getCampaignById(id: Long): Campaign? {
        return campaignDao.getCampaignById(id)
    }
    
    fun startCampaign(campaignId: Long, simSubscriptionId: Int? = null, delayBetweenSms: Long = 2000L) {
        Log.d(TAG, "Starting campaign: $campaignId with SIM: $simSubscriptionId")
        currentSimSubscriptionId = simSubscriptionId
        
        viewModelScope.launch {
            val campaign = campaignDao.getCampaignById(campaignId) 
            if (campaign == null) {
                Log.e(TAG, "Campaign not found: $campaignId")
                return@launch
            }
            
            Log.d(TAG, "Campaign found: ${campaign.name}, total: ${campaign.totalCount}")
            
            // Update status to RUNNING
            campaignDao.updateCampaignStatus(campaignId, CampaignStatus.RUNNING)
            if (campaign.startedAt == null) {
                campaignDao.updateCampaign(campaign.copy(
                    status = CampaignStatus.RUNNING,
                    startedAt = System.currentTimeMillis()
                ))
            }
            _currentCampaign.value = campaign.copy(status = CampaignStatus.RUNNING)
            _isSending.value = true
            
            Log.d(TAG, "Campaign status set to RUNNING")
            
            try {
                var messagesSent = 0
                
                while (_isSending.value) {
                    Log.d(TAG, "Fetching pending messages...")
                    val pendingMessages = campaignMessageDao.getPendingMessages(campaignId, limit = 1)
                    
                    Log.d(TAG, "Pending messages: ${pendingMessages.size}")
                    
                    if (pendingMessages.isEmpty()) {
                        Log.d(TAG, "No more pending messages, campaign completed")
                        campaignDao.updateCampaignStatus(campaignId, CampaignStatus.COMPLETED)
                        campaignDao.updateCampaign(
                            campaignDao.getCampaignById(campaignId)!!.copy(
                                completedAt = System.currentTimeMillis()
                            )
                        )
                        _isSending.value = false
                        break
                    }
                    
                    val message = pendingMessages.first()
                    Log.d(TAG, "Sending SMS to: ${message.phoneNumber}")
                    Log.d(TAG, "Message content: ${message.message}")
                    
                    val result = smsService.sendSms(
                        phoneNumber = message.phoneNumber, 
                        message = message.message,
                        subscriptionId = currentSimSubscriptionId
                    )
                    
                    when (result) {
                        is SmsResult.Success -> {
                            Log.d(TAG, "SMS sent successfully to: ${message.phoneNumber}")
                            campaignMessageDao.updateMessageStatus(
                                id = message.id,
                                status = MessageStatus.SENT,
                                sentAt = System.currentTimeMillis()
                            )
                            campaignDao.incrementSentCount(campaignId)
                            messagesSent++
                        }
                        is SmsResult.Failed -> {
                            Log.e(TAG, "SMS failed to: ${message.phoneNumber}, error: ${result.errorMessage}")
                            campaignMessageDao.updateMessageStatus(
                                id = message.id,
                                status = MessageStatus.FAILED,
                                errorMessage = result.errorMessage
                            )
                            campaignDao.incrementFailedCount(campaignId)
                        }
                    }
                    
                    // Update current campaign state
                    _currentCampaign.value = campaignDao.getCampaignById(campaignId)
                    
                    Log.d(TAG, "Messages sent so far: $messagesSent")
                    
                    // Delay before next message
                    delay(delayBetweenSms)
                }
                
                Log.d(TAG, "Campaign sending loop ended. Total sent: $messagesSent")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during campaign execution: ${e.message}", e)
                campaignDao.updateCampaignStatus(campaignId, CampaignStatus.FAILED)
                _isSending.value = false
            }
        }
    }
    
    fun pauseCampaign(campaignId: Long) {
        Log.d(TAG, "Pausing campaign: $campaignId")
        viewModelScope.launch {
            _isSending.value = false
            campaignDao.updateCampaignStatus(campaignId, CampaignStatus.PAUSED)
            _currentCampaign.value = campaignDao.getCampaignById(campaignId)
        }
    }
    
    fun resumeCampaign(campaignId: Long) {
        Log.d(TAG, "Resuming campaign: $campaignId")
        startCampaign(campaignId, currentSimSubscriptionId)
    }
}
