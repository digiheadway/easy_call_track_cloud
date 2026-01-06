import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { Todo, TodoType } from '../../types/o-index';
import { format, addDays, setHours, setMinutes } from 'date-fns';
import QuickDateTimeInput from '../common/QuickDateTimeInput';
import Badge from '../ui/Badge';

interface TodoActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  todo: Todo | null;
  actionType: 'complete' | 'cancel' | 'reschedule' | 'reopen';
}

  const TodoActionModal: React.FC<TodoActionModalProps> = ({ 
    isOpen, 
    onClose, 
    todo, 
    actionType 
  }) => {
    const { updateTodo, addTodo } = useAppContext();
    const [responseNote, setResponseNote] = useState('');
    const [rescheduleDate, setRescheduleDate] = useState<string>('');
    const [rescheduleTime, setRescheduleTime] = useState<string>('');
    const [createFollowUp, setCreateFollowUp] = useState(false);
    const [appendOnly, setAppendOnly] = useState(true);

    // Predefined suggestions for reschedule reasons
    const rescheduleSuggestions = [
      'Unable to Connect',
      'Asked to Reschedule', 
      'My Side Issue'
    ];

    // Predefined suggestions for cancel reasons
    const cancelSuggestions = [
      'Client Not Interested',
      'Budget Mismatch',
      'Timeline Issues',
      'Requirements Changed',
      'Competitor Selected',
      'Project Cancelled',
      'Client Unavailable',
      'Technical Constraints'
    ];

    // Reset checkbox when modal opens
    useEffect(() => {
      if (isOpen) {
        setCreateFollowUp(false);
        setResponseNote('');
        setAppendOnly(true);
      }
    }, [isOpen]);

    // Function to handle suggestion click
    const handleSuggestionClick = (suggestion: string, actionType: string) => {
      let formattedSuggestion = suggestion;
      
      // For reschedule, add date context
      if (actionType === 'reschedule') {
        const currentDate = format(new Date(), 'dd MMM');
        formattedSuggestion = `${suggestion} on ${currentDate}`;
      }
      
      if (appendOnly && responseNote) {
        // Append to existing text with "- " prefix for separation
        setResponseNote(prev => `${prev}, ${formattedSuggestion}`);
      } else {
        // Replace existing text with "- " prefix
        setResponseNote(`${formattedSuggestion}`);
      }
    };

  // Don't render if todo is null
  if (!todo) {
    return null;
  }

  // Initialize date and time if not set
  if (!rescheduleDate || !rescheduleTime) {
    const todoDate = new Date(todo.dateTime);
    setRescheduleDate(todoDate.toISOString().split('T')[0]);
    setRescheduleTime(format(todoDate, 'HH:mm'));
  }

  const handleComplete = async () => {
    // Update the current task
    await updateTodo(todo.id, {
      status: 'Completed',
      responseNote,
    });

    // Create follow-up task if checkbox is checked and it's a meeting task
    if (createFollowUp && todo.type === 'Meeting') {
      try {
        // Create follow-up task for tomorrow at 11 AM
        const tomorrow = addDays(new Date(), 1);
        const followUpDateTime = setMinutes(setHours(tomorrow, 11), 0);

        await addTodo({
          leadId: todo.leadId,
          type: 'Follow Up' as TodoType,
          description: `Ask Meeting feedback: ${todo.description || 'Meeting'}`,
          status: 'Pending',
          dateTime: followUpDateTime.toISOString(),
          participants: todo.participants,
        });
      } catch (error) {
        console.error('Failed to create follow-up task:', error);
        // Don't show error to user as the main task was completed successfully
      }
    }

    onClose();
  };

  const handleCancel = () => {
    updateTodo(todo.id, {
      status: 'Cancelled',
      responseNote,
    });
    onClose();
  };

  const handleReschedule = () => {
    const newDate = new Date(`${rescheduleDate}T${rescheduleTime}`);
    
    updateTodo(todo.id, {
      dateTime: newDate.toISOString(),
      responseNote,
      status: 'Pending',
    });
    onClose();
  };

  const handleReopen = () => {
    updateTodo(todo.id, {
      status: 'Pending',
      responseNote,
    });
    onClose();
  };

  const modalTitle = 
    actionType === 'complete' ? 'Complete Task' :
    actionType === 'cancel' ? 'Cancel Task' :
    actionType === 'reopen' ? 'Reopen Task' :
    'Reschedule Task';

  const buttonText = 
    actionType === 'complete' ? 'Done' :
    actionType === 'cancel' ? 'Cancel Task' :
    actionType === 'reopen' ? 'Reopen Task' :
    'Reschedule';

  const buttonClass = 
    actionType === 'complete' ? 'btn-success' :
    actionType === 'cancel' ? 'btn-danger' :
    actionType === 'reopen' ? 'btn-primary' :
    'btn-primary';

  const handleAction = () => {
    if (actionType === 'complete') {
      handleComplete();
    } else if (actionType === 'cancel') {
      handleCancel();
    } else if (actionType === 'reopen') {
      handleReopen();
    } else {
      handleReschedule();
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={modalTitle}
      size="md"
    >
      <div className="space-y-4">
        <div className="bg-gray-50 p-4 rounded-lg">
          {todo.description && <p className="text-sm text-gray-600">{todo.description}</p>}
          <p className="text-sm text-gray-500 mt-2">
            Scheduled for {format(new Date(todo.dateTime), 'PPp')}
          </p>
        </div>
        
        {actionType === 'reschedule' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <QuickDateTimeInput
                type="date"
                label="New Date"
                value={rescheduleDate}
                onChange={setRescheduleDate}
                min={new Date().toISOString().split('T')[0]}
              />
            </div>
            
            <div>
              <QuickDateTimeInput
                type="time"
                label="New Time"
                value={rescheduleTime}
                onChange={setRescheduleTime}
              />
            </div>
          </div>
        )}
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {actionType === 'reschedule' ? 'Reschedule Reason' : 
             actionType === 'reopen' ? 'Reopen Reason' :
             'Response Note'}
          </label>
          
          {/* Show suggestions for reschedule and cancel actions */}
          {(actionType === 'reschedule' || actionType === 'cancel') && (
            <div className="mb-3">
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs text-gray-500">Quick suggestions:</p>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="appendOnly"
                    checked={appendOnly}
                    onChange={(e) => setAppendOnly(e.target.checked)}
                    className="h-3 w-3 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="appendOnly" className="text-xs text-gray-600">
                    Append
                  </label>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {(actionType === 'reschedule' ? rescheduleSuggestions : cancelSuggestions).map((suggestion, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => handleSuggestionClick(suggestion, actionType)}
                    className="px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded-md hover:bg-blue-100 transition-colors border border-blue-200"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          )}
          
          <textarea
            value={responseNote}
            onChange={(e) => setResponseNote(e.target.value)}
            className="input h-32"
            placeholder={
              actionType === 'complete' ? 'Add details about task completion...' :
              actionType === 'cancel' ? 'Add reason for cancellation...' :
              actionType === 'reopen' ? 'Add reason for reopening...' :
              'Add reason for rescheduling...'
            }
          />
        </div>

        {/* Show follow-up checkbox only for meeting tasks when completing */}
        {actionType === 'complete' && todo.type === 'Meeting' && (
          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="createFollowUp"
              checked={createFollowUp}
              onChange={(e) => setCreateFollowUp(e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="createFollowUp" className="text-sm text-gray-700">
              Add follow-up task for tomorrow at 11 AM
            </label>
          </div>
        )}
      </div>
      
      <div className="flex justify-end mt-6 space-x-3">
        <button 
          type="button"
          className="btn btn-outline"
          onClick={onClose}
        >
          Cancel
        </button>
        <button 
          type="button"
          className={`btn ${buttonClass}`}
          onClick={handleAction}
          disabled={actionType === 'reschedule' && (!rescheduleDate || !rescheduleTime)}
        >
          {buttonText}
        </button>
      </div>
    </Modal>
  );
};

export default TodoActionModal;