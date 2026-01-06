import { useState, useEffect, useCallback } from 'react';

interface PersistentState {
  filters: any[];
  currentPage: number;
  sortField?: string;
  sortDirection?: 'asc' | 'desc';
  searchQuery?: string;
  activeTab?: string;
  lastUpdated?: number;
  isManuallyCleared?: boolean;
}

export function usePersistentState(sectionKey: string) {
  // Initialize state from localStorage or defaults
  const [state, setState] = useState<PersistentState>(() => {
    const savedState = localStorage.getItem(`${sectionKey}_state`);
    if (savedState) {
      try {
        const parsedState = JSON.parse(savedState);
        // Add timestamp if not present
        if (!parsedState.lastUpdated) {
          parsedState.lastUpdated = Date.now();
        }
        // Ensure isManuallyCleared is false for existing data
        if (parsedState.isManuallyCleared === undefined) {
          parsedState.isManuallyCleared = false;
        }
        return parsedState;
      } catch (error) {
        console.warn('Failed to parse saved state, using defaults:', error);
      }
    }
    
    return {
      filters: [],
      currentPage: 1,
      sortField: 'created_at',
      sortDirection: 'desc',
      searchQuery: '',
      activeTab: 'today',
      lastUpdated: Date.now(),
      isManuallyCleared: false
    };
  });

  // Save state to localStorage whenever it changes
  useEffect(() => {
    const stateToSave = {
      ...state,
      lastUpdated: Date.now()
    };
    localStorage.setItem(`${sectionKey}_state`, JSON.stringify(stateToSave));
  }, [state, sectionKey]);

  const updateState = useCallback((updates: Partial<PersistentState>) => {
    setState(prev => ({ 
      ...prev, 
      ...updates,
      lastUpdated: Date.now(),
      isManuallyCleared: false // Reset manual clear flag when state is updated
    }));
  }, []);

  const resetState = useCallback(() => {
    const defaultState: PersistentState = {
      filters: [],
      currentPage: 1,
      sortField: 'created_at',
      sortDirection: 'desc',
      searchQuery: '',
      activeTab: 'today',
      lastUpdated: Date.now(),
      isManuallyCleared: true
    };
    setState(defaultState);
    localStorage.removeItem(`${sectionKey}_state`);
  }, [sectionKey]);

  const preserveState = useCallback(() => {
    // This method can be called to ensure state is preserved
    // when navigating between pages
    const savedState = localStorage.getItem(`${sectionKey}_state`);
    if (savedState) {
      try {
        const parsedState = JSON.parse(savedState);
        setState(parsedState);
      } catch (error) {
        console.warn('Failed to parse saved state:', error);
      }
    }
  }, [sectionKey]);

  const clearFilters = useCallback(() => {
    setState(prev => ({
      ...prev,
      filters: [],
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: false
    }));
  }, []);

  const clearSearch = useCallback(() => {
    setState(prev => ({
      ...prev,
      searchQuery: '',
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: false
    }));
  }, []);

  const clearAll = useCallback(() => {
    setState(prev => ({
      ...prev,
      filters: [],
      searchQuery: '',
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: true
    }));
    localStorage.removeItem(`${sectionKey}_state`);
  }, [sectionKey]);

  const addFilter = useCallback((filter: any) => {
    setState(prev => ({
      ...prev,
      filters: [...prev.filters, filter],
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: false
    }));
  }, []);

  const removeFilter = useCallback((index: number) => {
    setState(prev => ({
      ...prev,
      filters: prev.filters.filter((_, i) => i !== index),
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: false
    }));
  }, []);

  const updateFilters = useCallback((filters: any[]) => {
    setState(prev => ({
      ...prev,
      filters,
      currentPage: 1,
      lastUpdated: Date.now(),
      isManuallyCleared: false
    }));
  }, []);

  const isStatePersisted = useCallback(() => {
    return !state.isManuallyCleared && state.lastUpdated;
  }, [state.isManuallyCleared, state.lastUpdated]);

  return {
    state,
    updateState,
    resetState,
    preserveState,
    clearFilters,
    clearSearch,
    clearAll,
    addFilter,
    removeFilter,
    updateFilters,
    isStatePersisted
  };
}