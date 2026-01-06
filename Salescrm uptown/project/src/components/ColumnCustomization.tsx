import React from 'react';
import { X } from 'lucide-react';
import { Lead } from '../types/lead';

interface Column {
  key: keyof Lead;
  label: string;
  visible: boolean;
  editable?: boolean;
  sortable?: boolean;
  minWidth?: number;
}

interface ColumnCustomizationProps {
  isOpen: boolean;
  onClose: () => void;
  columns: Column[];
  onColumnsChange: (columns: Column[]) => void;
}

export const ColumnCustomization: React.FC<ColumnCustomizationProps> = ({
  isOpen,
  onClose,
  columns,
  onColumnsChange,
}) => {
  if (!isOpen) return null;

  const handleToggleColumn = (key: keyof Lead) => {
    const updatedColumns = columns.map(col => 
      col.key === key ? { ...col, visible: !col.visible } : col
    );
    onColumnsChange(updatedColumns);
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-end md:items-center justify-center">
      <div className="bg-white w-full md:max-w-md md:rounded-lg max-h-[90vh] flex flex-col rounded-t-[20px]">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Customize Columns</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <div className="p-4 max-h-[60vh] overflow-y-auto">
          <div className="space-y-2">
            {columns.map((column) => (
              <label key={column.key} className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded-lg">
                <input
                  type="checkbox"
                  checked={column.visible}
                  onChange={() => handleToggleColumn(column.key)}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm">{column.label}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="border-t p-4 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};