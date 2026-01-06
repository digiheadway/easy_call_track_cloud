import { format, isToday, isTomorrow, isYesterday } from 'date-fns';

export const formatDateTime = (dateString: string | null): string => {
  if (!dateString) return 'Not Set';
  
  const date = new Date(dateString);
  
  if (isNaN(date.getTime())) return 'Not Set';
  
  if (isToday(date)) {
    return `Today ${format(date, 'h:mm a')}`;
  }
  
  if (isTomorrow(date)) {
    return `Tomorrow ${format(date, 'h:mm a')}`;
  }

  if (isYesterday(date)) {
    return `Yesterday ${format(date, 'h:mm a')}`;
  }
  
  return format(date, 'd MMMM h:mm a');
};