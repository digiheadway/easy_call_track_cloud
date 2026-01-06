import React, { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { Todo, DropdownOption } from '../../types';
import { dropdownOptions } from '../../data/options';
import Dropdown from '../common/Dropdown';
import TagInput from '../common/TagInput';
import QuickDateTimeInput from '../common/QuickDateTimeInput';

interface AddTodoModalProps {
  isOpen: boolean;
  onClose: () => void;
  leadId?: number;
  defaultType?: Todo['type'];
}

const AddTodoModal: React.FC<AddTodoModalProps> = ({
  isOpen,
  onClose,
  leadId,
  defaultType
}) => {
  const { addTodo, leads, options } = useAppContext();
  const navigate = useNavigate();

  const getInitialTodoState = (): Omit<Todo, 'id' | 'createdAt' | 'updatedAt'> => {
    // Create tomorrow at 11 AM in local time
    const now = new Date();
    const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 11, 0, 0, 0);

    return {
      leadId: leadId || 0,
      type: defaultType || 'Follow Up',

      description: '',
      responseNote: '',
      status: 'Pending',
      dateTime: tomorrow.toISOString(),
      participants: [],
    };
  };

  const [formData, setFormData] = useState(getInitialTodoState);

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setFormData(getInitialTodoState());
    }
  }, [isOpen, leadId, defaultType]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleTagChange = (name: string, value: string[]) => {
    setFormData({ ...formData, [name]: value });
  };

  const handleDateChange = (dateValue: string) => {
    const newDate = new Date(`${dateValue}T${timeValue}`);
    setFormData({ ...formData, dateTime: newDate.toISOString() });
  };

  const handleTimeChange = (timeValue: string) => {
    const newDate = new Date(`${dateValue}T${timeValue}`);
    setFormData({ ...formData, dateTime: newDate.toISOString() });
  };

  const handleSubmit = async () => {
    await addTodo(formData);

    if (!leadId && formData.leadId) {
      navigate(`/leads/${formData.leadId}`);
    }

    setFormData(getInitialTodoState());
    onClose();
  };

  const isFormValid = () => {
    return (
      formData.leadId !== 0 &&
      formData.description.trim() !== ''
    );
  };

  const { dateValue, timeValue } = useMemo(() => {
    const currentDate = new Date(formData.dateTime);
    return {
      dateValue: currentDate.toISOString().split('T')[0],
      timeValue: `${String(currentDate.getHours()).padStart(2, '0')}:${String(currentDate.getMinutes()).padStart(2, '0')}`
    };
  }, [formData.dateTime]);

  const leadOptions: DropdownOption[] = leads.map((lead: { id: number; name: string }) => ({
    value: lead.id.toString(),
    label: `${lead.id}. ${lead.name}`,
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Add New Task"
      size="md"
    >
      <div className="space-y-4">
        {!leadId && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Connected Lead *</label>
            <Dropdown
              options={leadOptions}
              value={formData.leadId.toString()}
              onChange={(value: string) => setFormData({ ...formData, leadId: parseInt(value) })}
              placeholder="Select a lead"
            />
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Task Type *</label>
          <Dropdown
            options={dropdownOptions.todoType}
            value={formData.type}
            onChange={(value: string) => setFormData({ ...formData, type: value as Todo['type'] })}
            placeholder="Select task type"
          />
        </div>



        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            className="input h-24"
            placeholder="Enter task description"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <QuickDateTimeInput
              type="date"
              label="Date *"
              value={dateValue}
              onChange={handleDateChange}
              required
            />
          </div>

          <div>
            <QuickDateTimeInput
              type="time"
              label="Time *"
              value={timeValue}
              onChange={handleTimeChange}
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Participants</label>
          <TagInput
            options={options.participants}
            value={formData.participants}
            onChange={(value: string[]) => handleTagChange('participants', value)}
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
          Add Task
        </button>
      </div>
    </Modal>
  );
};

export default AddTodoModal;