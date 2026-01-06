import React, { useState, useEffect } from 'react';
import { X, GripVertical, Eye, EyeOff } from 'lucide-react';
import { Lead } from '../../types';

export interface ColumnConfig {
  id: string;
  label: string;
  visible: boolean;
  order: number;
  alwaysVisible?: boolean;
}

interface ColumnSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  columns: ColumnConfig[];
  onColumnsChange: (columns: ColumnConfig[]) => void;
}

const ColumnSettingsModal: React.FC<ColumnSettingsModalProps> = ({
  isOpen,
  onClose,
  columns,
  onColumnsChange,
}) => {
  const [localColumns, setLocalColumns] = useState<ColumnConfig[]>(columns);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

  useEffect(() => {
    setLocalColumns(columns);
  }, [columns]);

  const handleToggleVisibility = (columnId: string) => {
    const updatedColumns = localColumns.map(col =>
      col.id === columnId && !col.alwaysVisible ? { ...col, visible: !col.visible } : col
    );
    setLocalColumns(updatedColumns);
  };

  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = (e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    
    if (draggedIndex === null || draggedIndex === dropIndex) {
      setDraggedIndex(null);
      return;
    }

    const newColumns = [...localColumns];
    const draggedColumn = newColumns[draggedIndex];
    
    // Remove dragged item
    newColumns.splice(draggedIndex, 1);
    
    // Insert at new position
    newColumns.splice(dropIndex, 0, draggedColumn);
    
    // Update order values
    const updatedColumns = newColumns.map((col, index) => ({
      ...col,
      order: index
    }));
    
    setLocalColumns(updatedColumns);
    setDraggedIndex(null);
  };

  const handleSave = () => {
    onColumnsChange(localColumns);
    onClose();
  };

  const handleReset = () => {
    const defaultColumns = getDefaultColumns();
    setLocalColumns(defaultColumns);
  };

  const getDefaultColumns = (): ColumnConfig[] => [
    { id: 'id', label: 'ID', visible: true, order: 0, alwaysVisible: true },
    { id: 'name', label: 'Name', visible: true, order: 1 },
    { id: 'budget', label: 'Budget', visible: true, order: 2 },
    { id: 'stage', label: 'Stage', visible: true, order: 3 },
    { id: 'tags', label: 'Tags', visible: true, order: 4 },
    { id: 'note', label: 'Note', visible: true, order: 5 },
    { id: 'priority', label: 'Priority', visible: true, order: 6 },
    { id: 'tasks', label: 'Tasks', visible: true, order: 7 },
    { id: 'type', label: 'Type', visible: true, order: 8 },
    { id: 'requirements', label: 'Requirements', visible: true, order: 9 },
    { id: 'address', label: 'Address', visible: false, order: 10 },
    { id: 'purpose', label: 'Purpose', visible: false, order: 11 },
    { id: 'about', label: 'About', visible: false, order: 12 },
    { id: 'segment', label: 'Segment', visible: false, order: 13 },
    { id: 'intent', label: 'Intent', visible: false, order: 14 },
    { id: 'assignedTo', label: 'Assigned To', visible: false, order: 15 },
    { id: 'listName', label: 'List Name', visible: false, order: 16 },
    { id: 'data1', label: 'Data 1', visible: false, order: 17 },
    { id: 'location', label: 'Location', visible: false, order: 18 },
    { id: 'actions', label: 'Actions', visible: true, order: 19, alwaysVisible: true },
  ];

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 transition-opacity" onClick={onClose}>
          <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
        </div>

        <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
          <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-medium text-gray-900">
                Customize Columns
              </h3>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>

            <div className="space-y-2 max-h-96 overflow-y-auto">
              <p className="text-sm text-gray-600 mb-4">
                Drag to reorder columns and toggle visibility
              </p>
              
              {localColumns
                .sort((a, b) => a.order - b.order)
                .map((column, index) => (
                  <div
                    key={column.id}
                    draggable
                    onDragStart={(e) => handleDragStart(e, index)}
                    onDragOver={handleDragOver}
                    onDrop={(e) => handleDrop(e, index)}
                    className={`flex items-center justify-between p-3 border rounded-lg cursor-move transition-colors ${
                      draggedIndex === index
                        ? 'bg-blue-50 border-blue-200'
                        : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <GripVertical size={16} className="text-gray-400" />
                      <span className="text-sm font-medium text-gray-900">
                        {column.label}
                      </span>
                    </div>
                    
                    <button
                      onClick={() => handleToggleVisibility(column.id)}
                      disabled={column.alwaysVisible}
                      className={`p-1 rounded-full transition-colors ${
                        column.alwaysVisible
                          ? 'text-gray-300 cursor-not-allowed'
                          : column.visible
                          ? 'text-blue-600 hover:bg-blue-100'
                          : 'text-gray-400 hover:bg-gray-100'
                      }`}
                      title={column.alwaysVisible ? 'This column is always visible' : ''}
                    >
                      {column.visible ? <Eye size={16} /> : <EyeOff size={16} />}
                    </button>
                  </div>
                ))}
            </div>
          </div>

          <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
            <button
              onClick={handleSave}
              className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-blue-600 text-base font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 sm:ml-3 sm:w-auto sm:text-sm"
            >
              Save Changes
            </button>
            <button
              onClick={handleReset}
              className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
            >
              Reset to Default
            </button>
            <button
              onClick={onClose}
              className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ColumnSettingsModal;
