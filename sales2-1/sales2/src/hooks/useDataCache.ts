import { useRef, useCallback, useMemo } from 'react';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  params: any;
}

interface CacheConfig {
  ttl?: number; // Time to live in milliseconds
  maxSize?: number; // Maximum number of cache entries
}

export function useDataCache<T>(config: CacheConfig = {}) {
  const { ttl = 5 * 60 * 1000, maxSize = 50 } = config; // Default 5 minutes TTL
  const cache = useRef<Map<string, CacheEntry<T>>>(new Map());

  const getCacheKey = useCallback((params: any): string => {
    return JSON.stringify(params);
  }, []);

  const isExpired = useCallback((entry: CacheEntry<T>): boolean => {
    return Date.now() - entry.timestamp > ttl;
  }, [ttl]);

  const getCachedData = useCallback((params: any): T | null => {
    const key = getCacheKey(params);
    const entry = cache.current.get(key);
    
    if (!entry || isExpired(entry)) {
      if (entry) {
        cache.current.delete(key);
      }
      return null;
    }
    
    return entry.data;
  }, [getCacheKey, isExpired]);

  const setCachedData = useCallback((params: any, data: T): void => {
    const key = getCacheKey(params);
    
    // Remove oldest entries if cache is full
    if (cache.current.size >= maxSize) {
      const oldestKey = cache.current.keys().next().value;
      if (oldestKey) {
        cache.current.delete(oldestKey);
      }
    }
    
    cache.current.set(key, {
      data,
      timestamp: Date.now(),
      params
    });
  }, [getCacheKey, maxSize]);

  const clearCache = useCallback((): void => {
    cache.current.clear();
  }, []);

  const clearExpiredEntries = useCallback((): void => {
    for (const [key, entry] of cache.current.entries()) {
      if (isExpired(entry)) {
        cache.current.delete(key);
      }
    }
  }, [isExpired]);

  const getCacheStats = useMemo(() => {
    clearExpiredEntries();
    return {
      size: cache.current.size,
      maxSize,
      ttl
    };
  }, [clearExpiredEntries, maxSize, ttl]);

  return {
    getCachedData,
    setCachedData,
    clearCache,
    clearExpiredEntries,
    getCacheStats
  };
} 