export interface FilterState {
  stage?: string;
  priority?: string;
  next_action?: string;
  preferred_type?: string;
  budget_min?: string;
  budget_max?: string;
  source?: string;
  segment?: string;
  purposes?: string;
  next_action_time?: string;
  custom_date?: string;
  assigned_to?: string;
  tags?: string[];
}

export interface FilterDisplayProps {
  filters: FilterState;
  onRemoveFilter: (key: string) => void;
  onClearFilters: () => void;
}

export const getFilterCount = (filters: FilterState): number => {
  return Object.keys(filters).filter(key => {
    const value = filters[key];
    return value !== undefined && 
           value !== null && 
           (Array.isArray(value) ? value.length > 0 : value !== '');
  }).length;
};

export const formatNextActionTimeFilter = (value: string): string => {
  switch (value) {
    case 'set':
      return 'Next Action: Set';
    case 'not_set':
      return 'Next Action: Not Set';
    case 'today':
      return 'Next Action: Today';
    case 'upcoming_3_days':
      return 'Next Action: Within 3 Days';
    case 'upcoming_7_days':
      return 'Next Action: Within 7 Days';
    case 'this_month':
      return 'Next Action: This Month';
    default:
      if (value && !['custom'].includes(value)) {
        return `Next Action: ${new Date(value).toLocaleDateString()}`;
      }
      return '';
  }
};