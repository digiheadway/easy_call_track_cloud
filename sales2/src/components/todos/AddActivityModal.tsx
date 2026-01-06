import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { Todo, DropdownOption } from '../../types';
import { tagOptions } from '../../data/options';
import TagInput from '../common/TagInput';

interface AddActivityModalProps {
  isOpen: boolean;
  onClose: () => void;
  leadId?: number;
}

const AddActivityModal: React.FC<AddActivityModalProps> = ({ 
  isOpen, 
  onClose, 
  leadId
}) => {
  const { addTodo, leads } = useAppContext();
  const navigate = useNavigate();
  
  const initialActivityState: Omit<Todo, 'id' | 'createdAt' | 'updatedAt'> = {
    leadId: leadId || 0,
    type: 'Activity',

    description: '',
    responseNote: '',
    status: 'Completed',
    dateTime: new Date().toISOString(), // Use current time for activities
    participants: [],
  };

  const [formData, setFormData] = useState(initialActivityState);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleTagChange = (name: string, value: string[]) => {
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async () => {
    await addTodo(formData);
    
    if (!leadId && formData.leadId) {
      navigate(`/leads/${formData.leadId}`);
    }
    
    setFormData(initialActivityState);
    onClose();
  };

  const isFormValid = () => {
    return (
      formData.leadId !== 0 &&
      formData.description.trim() !== ''
    );
  };

  const leadOptions: DropdownOption[] = leads.map(lead => ({
    value: lead.id.toString(),
    label: `${lead.id}. ${lead.name}`,
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Add Activity"
      size="md"
    >
      <div className="space-y-4">
        {!leadId && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Connected Lead *</label>
            <select
              value={formData.leadId}
              onChange={(e) => setFormData({ ...formData, leadId: parseInt(e.target.value) })}
              className="input"
            >
              <option value={0}>Select a lead</option>
              {leads.map(lead => (
                <option key={lead.id} value={lead.id}>
                  {lead.id}. {lead.name}
                </option>
              ))}
            </select>
          </div>
        )}
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description *</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            className="input h-24"
            placeholder="Enter activity details"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Participants</label>
          <TagInput
            options={tagOptions.participants}
            value={formData.participants}
            onChange={(value) => handleTagChange('participants', value)}
            placeholder="Add participants"
          />
        </div>
      </div>
      
      <div className="flex justify-end mt-6">
        <button 
          type="button"
          className="btn btn-primary"
          onClick={handleSubmit}
          disabled={!isFormValid()}
        >
          Add Activity
        </button>
      </div>
    </Modal>
  );
};

export default AddActivityModal;