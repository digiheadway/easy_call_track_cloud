import React, { useState, useEffect, useRef } from 'react';
import { Lead } from '../types/lead';
import { X, Phone, MessageCircle, Share2, Trash2 } from 'lucide-react';
import { TagInput } from './TagInput';
import toast from 'react-hot-toast';
import { format } from 'date-fns';
import { sendWebhook, shouldTriggerWebhook } from '../utils/webhook';
import { 
  LEAD_STAGES, 
  NEXT_ACTIONS, 
  PROPERTY_TYPES, 
  PURPOSES, 
  PRIORITIES, 
  SEGMENTS,
  TAGS,
  ASSIGNEES,
  SOURCES,
  MEDIUMS,
  LISTS,
  PLACEMENTS,
  DEFAULT_LEAD_VALUES 
} from '../types/options';

interface LeadFormProps {
  lead: Partial<Lead>;
  onClose: () => void;
  onSubmit: (lead: Partial<Lead>) => void;
  onDelete?: (id: number) => Promise<void>;
}

export const LeadForm: React.FC<LeadFormProps> = ({ 
  lead, 
  onClose, 
  onSubmit,
  onDelete 
}) => {
  const isNewLead = !lead.id;
  const [activeTab, setActiveTab] = useState(lead.id ? 'followUp' : 'personal');
  const [formData, setFormData] = useState(lead);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [lastSavedData, setLastSavedData] = useState(lead);
  const [pendingChanges, setPendingChanges] = useState<Partial<Lead>>({});
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [touchEnd, setTouchEnd] = useState<number | null>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (lead.id) {
      localStorage.setItem('lastOpenedLead', JSON.stringify(lead));
    }
  }, [lead]);

  const tabs = [
    { id: 'followUp', label: 'Follow Up' },
    { id: 'requirements', label: 'Requirements' },
    { id: 'personal', label: 'Personal' },
    { id: 'additional', label: 'Additional' }
  ];

  const minSwipeDistance = 50;

  const onTouchStart = (e: React.TouchEvent) => {
    setTouchEnd(null);
    setTouchStart(e.targetTouches[0].clientX);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };

  const onTouchEnd = () => {
    if (!touchStart || !touchEnd) return;

    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > minSwipeDistance;
    const isRightSwipe = distance < -minSwipeDistance;

    const currentTabIndex = tabs.findIndex(tab => tab.id === activeTab);

    if (isLeftSwipe && currentTabIndex < tabs.length - 1) {
      setActiveTab(tabs[currentTabIndex + 1].id);
    } else if (isRightSwipe && currentTabIndex > 0) {
      setActiveTab(tabs[currentTabIndex - 1].id);
    }
  };

  useEffect(() => {
    document.body.classList.add('modal-open');
    return () => {
      document.body.classList.remove('modal-open');
    };
  }, []);

  useEffect(() => {
    const handleClipboardData = async () => {
      try {
        const text = await navigator.clipboard.readText();
        const phoneNumber = text.replace(/[\s+]/g, '');
        if (/^\d{10,}$/.test(phoneNumber)) {
          let processedNumber = phoneNumber;
          if (phoneNumber.length > 10 && phoneNumber.startsWith('91')) {
            processedNumber = phoneNumber.slice(2);
          }
          if (processedNumber.length === 10) {
            handleFieldChange('phone', processedNumber);
          }
        }
      } catch (err) {
        // Silent fail if clipboard access is denied
      }
    };

    if (!lead.id) {
      handleClipboardData();
    }
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const modalElement = document.querySelector('.modal-content');
      const deleteModalElement = document.querySelector('.delete-modal-content');
      
      if (deleteModalElement?.contains(event.target as Node)) {
        return;
      }
      
      if (modalElement && !modalElement.contains(event.target as Node) && !showDeleteConfirm) {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose, showDeleteConfirm]);

  useEffect(() => {
    setFormData(lead.id ? lead : { ...DEFAULT_LEAD_VALUES, ...lead });
    setLastSavedData(lead);
    setPendingChanges({});
    setActiveTab(lead.id ? 'followUp' : 'personal');
  }, [lead]);

  const handleFieldChange = (field: keyof Lead, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (!isNewLead) {
      setPendingChanges(prev => ({ ...prev, [field]: value }));
    }
  };

  const handleFieldBlur = async (field: keyof Lead) => {
    if (!lead.id || isSubmitting || !pendingChanges[field]) return;

    const hasChanged = pendingChanges[field] !== lastSavedData[field];
    if (!hasChanged) return;

    try {
      setIsSubmitting(true);
      
      await onSubmit({ 
        ...formData,
        id: lead.id
      });

      if (shouldTriggerWebhook(field, false)) {
        try {
          await sendWebhook(formData);
        } catch (webhookError) {
          console.error('Webhook failed:', webhookError);
        }
      }

      setLastSavedData(prev => ({ ...prev, [field]: pendingChanges[field] }));
      setPendingChanges(prev => {
        const { [field]: _, ...rest } = prev;
        return rest;
      });
      
      toast.success('Updated successfully', { duration: 2000 });
    } catch (error) {
      console.error('Update failed:', error);
      setFormData(prev => ({ ...prev, [field]: lastSavedData[field] }));
      toast.error('Failed to update. Please try again.', { duration: 2000 });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleImmediateUpdate = async (field: keyof Lead, value: any) => {
    if (isNewLead) {
      handleFieldChange(field, value);
      return;
    }

    if (!lead.id || isSubmitting) return;

    try {
      setIsSubmitting(true);
      setFormData(prev => ({ ...prev, [field]: value }));
      
      const updatedFormData = {
        ...formData,
        [field]: value,
        id: lead.id
      };

      await onSubmit(updatedFormData);

      if (shouldTriggerWebhook(field, false)) {
        try {
          await sendWebhook(updatedFormData);
        } catch (webhookError) {
          console.error('Webhook failed:', webhookError);
        }
      }

      setLastSavedData(prev => ({ ...prev, [field]: value }));
      toast.success('Updated successfully', { duration: 2000 });
    } catch (error) {
      console.error('Update failed:', error);
      setFormData(prev => ({ ...prev, [field]: lastSavedData[field] }));
      toast.error('Failed to update. Please try again.', { duration: 2000 });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lead.id && !isSubmitting) {
      try {
        setIsSubmitting(true);
        await onSubmit(formData);
        toast.success('Lead created successfully', { duration: 2000 });
        onClose();
      } catch (error) {
        console.error('Failed to create lead:', error);
        toast.error('Failed to create lead', { duration: 2000 });
      } finally {
        setIsSubmitting(false);
      }
    }
  };

  const handleDelete = async () => {
    if (!lead.id || !onDelete || isDeleting) return;

    try {
      setIsDeleting(true);
      await onDelete(lead.id);
      toast.success('Lead deleted successfully', { duration: 2000 });
      onClose();
    } catch (error) {
      console.error('Error deleting lead:', error);
      toast.error('Failed to delete lead. Please try again.', { duration: 5000 });
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const handleCall = () => {
    if (formData.phone) {
      window.location.href = `tel:${formData.phone}`;
    } else {
      toast.error('No phone number available');
    }
  };

  const handleWhatsApp = () => {
    if (formData.phone) {
      const phone = formData.phone.replace(/\D/g, '');
      const text = `🏢 *Lead Details* #${formData.id}\n\n` +
        `👤 *Name:* ${formData.name || 'Not Set'}\n` +
        `📞 *Phone:* ${formData.phone}\n` +
        `📍 *Address:* ${formData.address || 'Not Set'}\n` +
        `💰 *Budget:* ${formData.budget ? `${formData.budget}L` : 'Not Set'}\n\n` +
        `📝 *Requirement:*\n_${formData.requirement_description || 'Not Set'}_`;
      
      window.open(`https://wa.me/${phone}?text=${encodeURIComponent(text)}`, '_blank');
    } else {
      toast.error('No phone number available');
    }
  };

  const handleShare = () => {
    const text = `🏢 Lead Details #${formData.id}\n\n` +
      `👤 Name: ${formData.name || 'Not Set'}\n` +
      `📞 Phone: ${formData.phone || 'Not Set'}\n` +
      `📍 Address: ${formData.address || 'Not Set'}\n` +
      `💰 Budget: ${formData.budget ? `${formData.budget}L` : 'Not Set'}\n\n` +
      `📝 Requirement:\n${formData.requirement_description || 'Not Set'}`;
    
    if (navigator.share) {
      navigator.share({
        title: 'Lead Details',
        text: text
      }).catch(() => {
        navigator.clipboard.writeText(text);
        toast.success('Copied to clipboard');
      });
    } else {
      navigator.clipboard.writeText(text);
      toast.success('Copied to clipboard');
    }
  };

  const formatDateTimeForInput = (dateString: string | null): string => {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    return format(date, "yyyy-MM-dd'T'HH:mm");
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-end md:items-center justify-center z-50">
      <div 
        ref={modalRef}
        className="modal-content bg-white w-full md:w-[600px] md:rounded-lg h-[90vh] md:h-[80vh] overflow-hidden flex flex-col relative animate-slideIn rounded-t-[20px] md:rounded-lg"
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        <div className="flex items-center justify-between p-4 border-b bg-white sticky top-0 z-20">
          <div>
            <h2 className="text-lg font-semibold text-rich">
              {lead.id ? `#${lead.id} ${lead.name}` : 'Add New Lead'}
            </h2>
            {lead.id && <p className="text-medium">{lead.segment} - {lead.budget}</p>}
          </div>
          <button 
            onClick={onClose} 
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <X size={20} className="text-medium" />
          </button>
        </div>

        <div className="flex overflow-x-auto border-b bg-white sticky top-[73px] z-10 scrollbar-hide">
          {tabs.map(tab => (
            <button
              key={tab.id}
              className={`px-4 py-2 whitespace-nowrap transition-colors ${
                activeTab === tab.id ? 'border-b-2 border-blue-500 text-blue-600' : 'hover:bg-gray-50'
              }`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto">
          <form onSubmit={handleSubmit} className="p-4">
            {activeTab === 'requirements' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Requirement Description</label>
                  <textarea rows="2"
                    className="w-full p-2 border rounded-lg min-h-[2px]"
                    value={formData.requirement_description || ''}
                    onChange={(e) => handleFieldChange('requirement_description', e.target.value)}
                    onBlur={() => handleFieldBlur('requirement_description')}
                    placeholder="Enter requirement details..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Budget (Lakhs)</label>
                  <input
                    type="number"
                    className="w-full p-2 border rounded-lg"
                    value={formData.budget || ''}
                    onChange={(e) => handleFieldChange('budget', Number(e.target.value))}
                    onBlur={() => handleFieldBlur('budget')}
                    placeholder="Enter budget in lakhs"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Property Types</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.preferred_type || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('preferred_type', e.target.value) : handleImmediateUpdate('preferred_type', e.target.value)}
                  >
                    <option value="">Select Property Type</option>
                    {PROPERTY_TYPES.map(type => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Purpose</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.purposes || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('purposes', e.target.value) : handleImmediateUpdate('purposes', e.target.value)}
                  >
                    <option value="">Select Purpose</option>
                    {PURPOSES.map(purpose => (
                      <option key={purpose} value={purpose}>{purpose}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Preferred Area</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.preferred_area || ''}
                    onChange={(e) => handleFieldChange('preferred_area', e.target.value)}
                    onBlur={() => handleFieldBlur('preferred_area')}
                    placeholder="Enter preferred area"
                  />
                </div>
              </div>
            )}

            {activeTab === 'followUp' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Stage</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.stage || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('stage', e.target.value) : handleImmediateUpdate('stage', e.target.value)}
                  >
                    <option value="">Not Set</option>
                    {LEAD_STAGES.map(stage => (
                      <option key={stage} value={stage}>{stage}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Next Action</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.next_action || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('next_action', e.target.value) : handleImmediateUpdate('next_action', e.target.value)}
                  >
                    <option value="">Select Action</option>
                    {NEXT_ACTIONS.map(action => (
                      <option key={action} value={action}>{action}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Next Action Time</label>
                  <input
                    type="datetime-local"
                    className="w-full p-2 border rounded-lg"
                    value={formatDateTimeForInput(formData.next_action_time)}
                    onChange={(e) => handleFieldChange('next_action_time', e.target.value)}
                    onBlur={() => handleFieldBlur('next_action_time')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Action Notes</label>
                  <textarea rows="1"
                    className="w-full p-2 border rounded-lg min-h-[2px]"
                    value={formData.next_action_note || ''}
                    onChange={(e) => handleFieldChange('next_action_note', e.target.value)}
                    onBlur={() => handleFieldBlur('next_action_note')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                  <textarea rows="3"
                    className="w-full p-2 border rounded-lg min-h-[1px]"
                    value={formData.note || ''}
                    onChange={(e) => handleFieldChange('note', e.target.value)}
                    onBlur={() => handleFieldBlur('note')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Intent</label>
                  <input
                    type="number"
                    className="w-full p-2 border rounded-lg"
                    min="1"
                    max="10"
                    value={formData.intent || ''}
                    onChange={(e) => handleFieldChange('intent', Number(e.target.value))}
                    onBlur={() => handleFieldBlur('intent')}
                  />
                </div>
              </div>
            )}

            {activeTab === 'personal' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.name || ''}
                    onChange={(e) => handleFieldChange('name', e.target.value)}
                    onBlur={() => handleFieldBlur('name')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
                  <input
                    type="tel"
                    className="w-full p-2 border rounded-lg"
                    value={formData.phone || ''}
                    onChange={(e) => handleFieldChange('phone', e.target.value)}
                    onBlur={() => handleFieldBlur('phone')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.address || ''}
                    onChange={(e) => handleFieldChange('address', e.target.value)}
                    onBlur={() => handleFieldBlur('address')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">About Person</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.about_him || ''}
                    onChange={(e) => handleFieldChange('about_him', e.target.value)}
                    onBlur={() => handleFieldBlur('about_him')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Alternative Contact</label>
                  <input
                    type="tel"
                    className="w-full p-2 border rounded-lg"
                    value={formData.alternative_contact_details || ''}
                    onChange={(e) => handleFieldChange('alternative_contact_details', e.target.value)}
                    onBlur={() => handleFieldBlur('alternative_contact_details')}
                  />
                </div>
              </div>
            )}

            {activeTab === 'additional' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Segment</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.segment || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('segment', e.target.value) : handleImmediateUpdate('segment', e.target.value)}
                  >
                    <option value="">Select Segment</option>
                    {SEGMENTS.map(segment => (
                      <option key={segment} value={segment}>{segment}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Source</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.source || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('source', e.target.value) : handleImmediateUpdate('source', e.target.value)}
                  >
                    <option value="">Select Source</option>
                    {SOURCES.map(source => (
                      <option key={source} value={source}>{source}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Medium</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.medium || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('medium', e.target.value) : handleImmediateUpdate('medium', e.target.value)}
                  >
                    <option value="">Select Medium</option>
                    {MEDIUMS.map(medium => (
                      <option key={medium} value={medium}>{medium}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
                  <select
                    className="w-full p-2 border rounded-lg"
                    value={formData.priority || ''}
                    onChange={(e) => isNewLead ? handleFieldChange('priority', Number(e.target.value)) : handleImmediateUpdate('priority', Number(e.target.value))}
                  >
                    <option value="">Select Priority</option>
                    {PRIORITIES.map(priority => (
                      <option key={priority.value} value={priority.value}>{priority.label}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">List Name</label>
                  <TagInput
                    value={(formData.list_name || '').split(',').filter(Boolean)}
                    onChange={(values) => isNewLead ? handleFieldChange('list_name', values.join(',')) : handleImmediateUpdate('list_name', values.join(','))}
                    suggestions={LISTS}
                    allowCustom={true}
                    placeholder="Add lists..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Assigned To</label>
                  <TagInput
                    value={(formData.assigned_to || '').split(',').filter(Boolean)}
                    onChange={(values) => isNewLead ? handleFieldChange('assigned_to', values.join(',')) : handleImmediateUpdate('assigned_to', values.join(','))}
                    suggestions={ASSIGNEES}
                    allowCustom={true}
                    placeholder="Add assignees..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Tags</label>
                  <TagInput
                    value={(formData.tags || '').split(',').filter(Boolean)}
                    onChange={(values) => isNewLead ? handleFieldChange('tags', values.join(',')) : handleImmediateUpdate('tags', values.join(','))}
                    suggestions={TAGS}
                    allowCustom={true}
                    placeholder="Add tags..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Custom Field 1</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.data_1 || ''}
                    onChange={(e) => handleFieldChange('data_1', e.target.value)}
                    onBlur={() => handleFieldBlur('data_1')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Custom Field 2</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.data_2 || ''}
                    onChange={(e) => handleFieldChange('data_2', e.target.value)}
                    onBlur={() => handleFieldBlur('data_2')}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Custom Field 3</label>
                  <input
                    type="text"
                    className="w-full p-2 border rounded-lg"
                    value={formData.data_3 || ''}
                    onChange={(e) => handleFieldChange('data_3', e.target.value)}
                    onBlur={() => handleFieldBlur('data_3')}
                  />
                </div>

                {lead.id && (
                  <div className="pt-4 border-t">
                    <button
                      type="button"
                      onClick={() => setShowDeleteConfirm(true)}
                      className="w-full px-4 py-2 text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors flex items-center justify-center gap-2"
                    >
                      <Trash2 size={20} />
                      Delete Lead
                    </button>
                  </div>
                )}
              </div>
            )}
          </form>
        </div>

        <div className="flex items-center justify-between p-4 border-t bg-white sticky bottom-0 shadow-top">
          {lead.id ? (
            <div className="flex gap-2 flex-1">
              <button
                onClick={handleShare}
                className="p-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
              >
                <Share2 size={20} />
              </button>
              
              <button
                onClick={handleWhatsApp}
                className="flex-1 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors flex items-center gap-2 justify-center"
              >
                <MessageCircle size={20} />
                WhatsApp
              </button>
              
              <button
                onClick={handleCall}
                className="flex-1 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors flex items-center gap-2 justify-center"
              >
                <Phone size={20} />
                Call
              </button>
            </div>
          ) : (
            <div className="flex gap-2 w-full">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-4 py-2 text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                onClick={handleSubmit}
              >
                {isSubmitting ? 'Creating...' : 'Create'}
              </button>
            </div>
          )}
        </div>
      </div>

      {showDeleteConfirm && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-[60]"
          onClick={(e) => {
            e.stopPropagation();
          }}
        >
          <div 
            className="delete-modal-content bg-white rounded-lg p-6 max-w-sm mx-4 w-full"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-semibold mb-2">Delete Lead</h3>
            <p className="text-gray-600 mb-4">
              Are you sure you want to delete this lead? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setShowDeleteConfirm(false);
                }}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete();
                }}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                disabled={isDeleting}
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};