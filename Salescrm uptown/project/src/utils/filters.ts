import { addDays, startOfDay, endOfDay, isValid } from 'date-fns';

export const parseNextActionTimeFilter = (params: URLSearchParams): Record<string, string> => {
  const filters: Record<string, string> = {};
  
  // Handle exact date
  const exactDate = params.get('next_action_time_exact');
  if (exactDate && isValid(new Date(exactDate))) {
    filters.next_action_time_exact = exactDate;
  }

  // Handle special filters
  ['set', 'not_set', '3_days', '7_days', 'month'].forEach(filter => {
    const value = params.get(`next_action_time_${filter}`);
    if (value === '1') {
      filters[`next_action_time_${filter}`] = '1';
    }
  });

  return filters;
};

export const getNextActionTimeQuery = (filters: Record<string, string>): string => {
  const conditions = [];

  if (filters.next_action_time_exact) {
    const date = new Date(filters.next_action_time_exact);
    if (isValid(date)) {
      conditions.push(`next_action_time BETWEEN '${startOfDay(date).toISOString()}' AND '${endOfDay(date).toISOString()}'`);
    }
  }

  if (filters.next_action_time_set === '1') {
    conditions.push('next_action_time IS NOT NULL');
  }

  if (filters.next_action_time_not_set === '1') {
    conditions.push('next_action_time IS NULL');
  }

  if (filters.next_action_time_3_days === '1') {
    conditions.push(`next_action_time <= '${addDays(new Date(), 3).toISOString()}'`);
  }

  if (filters.next_action_time_7_days === '1') {
    conditions.push(`next_action_time <= '${addDays(new Date(), 7).toISOString()}'`);
  }

  if (filters.next_action_time_month === '1') {
    conditions.push(`next_action_time <= '${addDays(new Date(), 30).toISOString()}'`);
  }

  return conditions.join(' AND ');
};