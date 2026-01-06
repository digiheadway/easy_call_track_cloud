import { Lead } from '../types/lead';

const CACHE_KEY = 'leads-cache';
const CACHE_TIMESTAMP_KEY = 'leads-cache-timestamp';
const CACHE_DURATION = 1000 * 60 * 60; // 1 hour

export interface CachedData {
  leads: Lead[];
  metadata: {
    total: number;
    page: number;
    per_page: number;
    total_pages: number;
  };
  timestamp: number;
}

export const saveToCache = async (data: CachedData) => {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(data));
    localStorage.setItem(CACHE_TIMESTAMP_KEY, Date.now().toString());
  } catch (error) {
    console.error('Error saving to cache:', error);
  }
};

export const getFromCache = (): CachedData | null => {
  try {
    const cachedData = localStorage.getItem(CACHE_KEY);
    const timestamp = localStorage.getItem(CACHE_TIMESTAMP_KEY);

    if (!cachedData || !timestamp) return null;

    // Check if cache is expired
    if (Date.now() - parseInt(timestamp) > CACHE_DURATION) {
      localStorage.removeItem(CACHE_KEY);
      localStorage.removeItem(CACHE_TIMESTAMP_KEY);
      return null;
    }

    return JSON.parse(cachedData);
  } catch (error) {
    console.error('Error reading from cache:', error);
    return null;
  }
};

export const clearCache = () => {
  try {
    localStorage.removeItem(CACHE_KEY);
    localStorage.removeItem(CACHE_TIMESTAMP_KEY);
  } catch (error) {
    console.error('Error clearing cache:', error);
  }
};