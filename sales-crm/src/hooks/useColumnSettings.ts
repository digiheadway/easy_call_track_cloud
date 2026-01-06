import { useState, useEffect, useCallback } from 'react';
import { ColumnConfig } from '../components/leads/ColumnSettingsModal';

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

export const useColumnSettings = (storageKey: string = 'leads-column-settings') => {
  const [columns, setColumns] = useState<ColumnConfig[]>(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (error) {
      console.warn('Failed to load column settings from localStorage:', error);
    }
    return getDefaultColumns();
  });

  const [isModalOpen, setIsModalOpen] = useState(false);

  // Save to localStorage whenever columns change
  useEffect(() => {
    try {
      localStorage.setItem(storageKey, JSON.stringify(columns));
    } catch (error) {
      console.warn('Failed to save column settings to localStorage:', error);
    }
  }, [columns, storageKey]);

  const updateColumns = useCallback((newColumns: ColumnConfig[]) => {
    setColumns(newColumns);
  }, []);

  const openModal = useCallback(() => {
    setIsModalOpen(true);
  }, []);

  const closeModal = useCallback(() => {
    setIsModalOpen(false);
  }, []);

  const resetToDefault = useCallback(() => {
    setColumns(getDefaultColumns());
  }, []);

  // Get visible columns in order
  const getVisibleColumns = useCallback(() => {
    return columns
      .filter(col => col.visible)
      .sort((a, b) => a.order - b.order);
  }, [columns]);

  // Get column by id
  const getColumnById = useCallback((id: string) => {
    return columns.find(col => col.id === id);
  }, [columns]);

  return {
    columns,
    updateColumns,
    isModalOpen,
    openModal,
    closeModal,
    resetToDefault,
    getVisibleColumns,
    getColumnById,
  };
};
