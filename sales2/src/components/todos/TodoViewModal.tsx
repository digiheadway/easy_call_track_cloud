import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { Todo } from '../../types';
import { format } from 'date-fns';
import Badge from '../ui/Badge';
import { dropdownOptions, tagOptions } from '../../data/options';
import Dropdown from '../common/Dropdown';
import TagInput from '../common/TagInput';
import QuickDateTimeInput from '../common/QuickDateTimeInput';
import TodoActionModal from './TodoActionModal';
import { Pencil, CheckCircle, XCircle, RotateCcw, RefreshCw } from 'lucide-react';

interface TodoViewModalProps {
  isOpen: boolean;
  onClose: () => void;
  todo: Todo | null;
}

const TodoViewModal: React.FC<TodoViewModalProps> = ({
  isOpen,
  onClose,
  todo,
}) => {
  const { getLeadById, updateTodo } = useAppContext();
  const navigate = useNavigate();

  const [editedTodo, setEditedTodo] = useState<Todo | null>(todo);
  const [isEditing, setIsEditing] = useState(false);
  const [isActionModalOpen, setIsActionModalOpen] = useState(false);
  const [actionType, setActionType] = useState<'complete' | 'cancel' | 'reschedule' | 'reopen'>('reschedule');

  // Update editedTodo when todo prop changes
  useEffect(() => {
    setEditedTodo(todo);
  }, [todo]);

  // Don't render if modal is not open
  if (!isOpen) {
    return null;
  }

  // Don't render if todo is null
  if (!todo || !editedTodo) {
    return null;
  }

  const lead = getLeadById(editedTodo.leadId);

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setEditedTodo((prev) => prev ? ({ ...prev, [name]: value }) : null);
  };

  const handleDropdownChange = (name: string, value: string) => {
    setEditedTodo((prev) => prev ? ({ ...prev, [name]: value }) : null);
  };

  const handleTagChange = (name: string, value: string[]) => {
    setEditedTodo((prev) => prev ? ({ ...prev, [name]: value }) : null);
  };

  const handleDateChange = (value: string) => {
    if (!editedTodo) return;
    const currentDateTime = new Date(editedTodo.dateTime);
    const [year, month, day] = value.split('-').map(Number);
    currentDateTime.setFullYear(year);
    currentDateTime.setMonth(month - 1);
    currentDateTime.setDate(day);
    setEditedTodo((prev) => prev ? ({
      ...prev,
      dateTime: currentDateTime.toISOString(),
    }) : null);
  };

  const handleTimeChange = (value: string) => {
    if (!editedTodo) return;
    const currentDateTime = new Date(editedTodo.dateTime);
    const [hours, minutes] = value.split(':').map(Number);
    currentDateTime.setHours(hours);
    currentDateTime.setMinutes(minutes);
    setEditedTodo((prev) => prev ? ({
      ...prev,
      dateTime: currentDateTime.toISOString(),
    }) : null);
  };

  const handleSave = () => {
    if (editedTodo) {
      updateTodo(todo.id, editedTodo);
      setIsEditing(false);
    }
  };

  const handleStatusChange = (
    newStatus: 'Completed' | 'Cancelled' | 'Pending'
  ) => {
    if (!editedTodo) return;
    
    const updatedTodo = {
      ...editedTodo,
      status: newStatus,
    };
    updateTodo(todo.id, updatedTodo);
    setEditedTodo(updatedTodo);
  };

  const currentDate = new Date(editedTodo.dateTime);
  const dateValue = currentDate.toISOString().split('T')[0];
  const timeValue = `${String(currentDate.getHours()).padStart(
    2,
    '0'
  )}:${String(currentDate.getMinutes()).padStart(2, '0')}`;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        isEditing
          ? 'Edit ' + '#'+ editedTodo.id
          : +editedTodo.id + ' - ' + editedTodo.type + ' Details'
      }
      size="lg"
    >
      <div className="space-y-4">
        <div>
        <div className="items-center justify-between">
  <div className="items-center space-x-2">
    {isEditing ? (
      <Dropdown
        options={dropdownOptions.todoType}
        value={editedTodo.type}
        onChange={(value) => handleDropdownChange('type', value)}
        placeholder="Select type"
      />
    ) : null}
  </div>
</div>


          {isEditing ? (
            <>

              <div className="mt-4">
                <label className="block text-xs md:text-sm font-semibold text-gray-600 mb-1">
                  Description
                </label>
                <textarea
                  name="description"
                  value={editedTodo.description}
                  onChange={handleInputChange}
                  className="input h-24"
                />
              </div>
            </>
          ) : (
            <>
              {editedTodo.description && (
                <p className="text-gray-600 mt-0 mb-4">
                  {editedTodo.description}
                </p>
              )}
              <button
                className="text-sm md:text-base text-blue-600 hover:text-blue-800 font-medium"
                onClick={() => {
                  navigate(`/leads/${editedTodo.leadId}`);
                  onClose();
                }}
              >
                {editedTodo.leadId}. {lead?.name} &{' '}
                {editedTodo.participants.map((participant, idx) => (
                  <Badge key={idx} label={participant} color="gray" small />
                ))}
              </button>
            </>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          {isEditing ? (
            <>
              <div>
                <QuickDateTimeInput
                  type="date"
                  label="Date"
                  value={dateValue}
                  onChange={handleDateChange}
                />
              </div>
              <div>
                <QuickDateTimeInput
                  type="time"
                  label="Time"
                  value={timeValue}
                  onChange={handleTimeChange}
                />
              </div>
            </>
          ) : (
            <div>
              <label className="block text-xs md:text-sm font-semibold text-gray-600">
                Date & Time
              </label>
              <p className="mt-1 text-sm md:text-base text-gray-800 font-medium whitespace-nowrap overflow-hidden text-ellipsis">
                {format(new Date(editedTodo.dateTime), 'dd MMM, h:mm a')}
              </p>
            </div>
          )}
        </div>
        <div>
          {isEditing && (
            <>
              <label className="block text-xs md:text-sm font-semibold text-gray-600 mb-1">
                Participants
              </label>
              <TagInput
                options={tagOptions.participants}
                value={editedTodo.participants}
                onChange={(value) => handleTagChange('participants', value)}
                placeholder="Add participants"
              />
            </>
          )}
        </div>

        <div>
          <label className="block text-xs md:text-sm font-semibold text-gray-600 mb-1">
            Response
          </label>
          {isEditing || editedTodo.status === 'Pending' ? (
            <textarea
              name="responseNote"
              value={editedTodo.responseNote}
              onChange={handleInputChange}
              className="input h-24"
              placeholder="Enter response or notes..."
            />
          ) : (
            <p className="mt-1 text-sm md:text-base text-gray-700">
              {editedTodo.responseNote || 'No response provided'}
            </p>
          )}
        </div>
        {!isEditing && (
          <div className="flex items-center justify-between">
            <div>
              <button
                className="p-1.5 text-gray-600 hover:text-gray-900 rounded-full hover:bg-gray-100"
                onClick={() => setIsEditing(true)}
                title="Edit"
              >
                <Pencil size={16} />
              </button>
            </div>
            <div className="flex space-x-1.5">
              {editedTodo.status === 'Pending' ? (
                <>
                  <button
                    className="p-1.5 text-gray-600 hover:text-green-600 rounded-full hover:bg-green-50"
                    onClick={() => handleStatusChange('Completed')}
                    title="Complete"
                  >
                    <CheckCircle size={18} />
                  </button>
                  <button
                    className="p-1.5 text-gray-600 hover:text-red-600 rounded-full hover:bg-red-50"
                    onClick={() => handleStatusChange('Cancelled')}
                    title="Cancel"z
                  >
                    <XCircle size={18} />
                  </button>
                  <button
                    className="p-1.5 text-gray-600 hover:text-orange-600 rounded-full hover:bg-orange-50"
                    onClick={() => { setActionType('reschedule'); setIsActionModalOpen(true); }}
                    title="Reschedule"
                  >
                    <RotateCcw size={18} />
                  </button>
                </>
              ) : (
                <button
                  className="p-1.5 text-gray-600 hover:text-blue-600 rounded-full hover:bg-blue-50"
                  onClick={() => handleStatusChange('Pending')}
                  title="Reopen"
                >
                  <RefreshCw size={18} />
                </button>
              )}
            </div>
          </div>
        )}
        {isEditing && (
          <div className="flex justify-end space-x-3">
            <button
              className="btn btn-outline"
              onClick={() => {
                setEditedTodo(todo);
                setIsEditing(false);
              }}
            >
              Cancel
            </button>
            <button className="btn btn-primary" onClick={handleSave}>
              Save Changes
            </button>
          </div>
        )}
      </div>
      <TodoActionModal
        isOpen={isActionModalOpen}
        onClose={() => setIsActionModalOpen(false)}
        todo={editedTodo}
        actionType={actionType}
      />
    </Modal>
  );
};

export default TodoViewModal;
