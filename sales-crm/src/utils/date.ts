import { format, isToday, isTomorrow, isThisYear } from 'date-fns';

export const formatDateTime = (dateString: string, formatStr: string = 'PPp') => {
  const date = new Date(dateString);
  return format(date, formatStr);
};

export const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  if (isToday(date)) {
    return `Today, ${format(date, 'h:mm a')}`;
  }
  if (isTomorrow(date)) {
    return `Tomorrow, ${format(date, 'h:mm a')}`;
  }
  const absoluteFormat = isThisYear(date) ? 'd MMM, h:mm a' : 'd MMM yyyy, h:mm a';
  return format(date, absoluteFormat);
};

export const formatDateForInput = (dateString: string) => {
  return format(new Date(dateString), 'yyyy-MM-dd');
};

export const formatTimeForInput = (dateString: string) => {
  return format(new Date(dateString), 'HH:mm');
};