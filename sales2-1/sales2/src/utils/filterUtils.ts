import { FilterOption } from '../types/o-index';

export interface FilterState {
  filters: FilterOption[];
  searchQuery: string;
  currentPage: number;
  sortField: string;
  sortDirection: 'asc' | 'desc';
  activeTab?: string;
}

export const createFilterKey = (filters: FilterOption[]): string => {
  return filters
    .map(filter => `${filter.field}:${filter.operator}:${filter.value}`)
    .sort()
    .join('|');
};

export const areFiltersEqual = (filters1: FilterOption[], filters2: FilterOption[]): boolean => {
  if (filters1.length !== filters2.length) return false;
  
  const key1 = createFilterKey(filters1);
  const key2 = createFilterKey(filters2);
  
  return key1 === key2;
};

export const mergeFilterStates = (
  persistentState: FilterState,
  contextFilters: FilterOption[]
): FilterState => {
  // If context has filters but persistent state doesn't, use context
  if (contextFilters.length > 0 && persistentState.filters.length === 0) {
    return {
      ...persistentState,
      filters: contextFilters,
      currentPage: 1 // Reset page when filters change
    };
  }
  
  // If persistent state has filters but context doesn't, use persistent
  if (persistentState.filters.length > 0 && contextFilters.length === 0) {
    return persistentState;
  }
  
  // If both have filters, prefer context but keep other persistent state
  if (contextFilters.length > 0) {
    return {
      ...persistentState,
      filters: contextFilters,
      currentPage: 1
    };
  }
  
  return persistentState;
};

export const saveFilterState = (sectionKey: string, state: FilterState): void => {
  try {
    const stateToSave = {
      ...state,
      lastUpdated: Date.now()
    };
    localStorage.setItem(`${sectionKey}_state`, JSON.stringify(stateToSave));
  } catch (error) {
    console.warn('Failed to save filter state:', error);
  }
};

export const loadFilterState = (sectionKey: string): FilterState | null => {
  try {
    const savedState = localStorage.getItem(`${sectionKey}_state`);
    if (savedState) {
      const parsedState = JSON.parse(savedState);
      return parsedState;
    }
  } catch (error) {
    console.warn('Failed to load filter state:', error);
  }
  return null;
};

export const clearFilterState = (sectionKey: string): void => {
  try {
    localStorage.removeItem(`${sectionKey}_state`);
  } catch (error) {
    console.warn('Failed to clear filter state:', error);
  }
}; 