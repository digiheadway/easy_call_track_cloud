import { isEqual } from 'lodash';
import { FilterOption } from '../types';

export function applyFilters<T>(items: T[], filters: FilterOption[]): T[] {
  if (filters.length === 0) return items;

  return items.filter((item) => {
    return filters.every((filter) => {
      const fieldValue = item[filter.field as keyof T];

      // Handle array values
      if (Array.isArray(fieldValue)) {
        if (Array.isArray(filter.value)) {
          return filter.value.some((v) => fieldValue.includes(v));
        }
        return fieldValue.includes(filter.value);
      }

      // Handle different operators
      switch (filter.operator) {
        case '=':
          return isEqual(fieldValue, filter.value);
        
        case 'contains':
          if (typeof fieldValue === 'string') {
            return fieldValue.toLowerCase().includes(String(filter.value).toLowerCase());
          }
          return false;
        
        case '>=':
          if (typeof fieldValue === 'number') {
            return fieldValue >= Number(filter.value);
          }
          return false;
        
        case '<=':
          if (typeof fieldValue === 'number') {
            return fieldValue <= Number(filter.value);
          }
          return false;
        
        default:
          return false;
      }
    });
  });
}