import { useState, useCallback, useRef, useEffect } from 'react';
import { useDataCache } from './useDataCache';

interface FetchConfig<T> {
  onSuccess?: (data: T) => void;
  onError?: (error: Error) => void;
  background?: boolean; // If true, don't show loading state
  forceRefresh?: boolean; // If true, bypass cache
  compareData?: (oldData: T, newData: T) => boolean; // Custom comparison function
}

export function useOptimizedDataFetching<T>(
  fetchFunction: (params: any) => Promise<T>,
  cacheConfig?: { ttl?: number; maxSize?: number }
) {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [isBackgroundLoading, setIsBackgroundLoading] = useState(false);
  
  const { getCachedData, setCachedData, clearCache } = useDataCache<T>(cacheConfig);
  const activeRequests = useRef<Map<string, Promise<T>>>(new Map());
  const lastParams = useRef<any>(null);

  const createRequestKey = useCallback((params: any): string => {
    return JSON.stringify(params);
  }, []);

  const isDataEqual = useCallback((oldData: T, newData: T): boolean => {
    if (oldData === newData) return true;
    if (!oldData || !newData) return false;
    
    // Deep comparison for arrays and objects
    if (Array.isArray(oldData) && Array.isArray(newData)) {
      if (oldData.length !== newData.length) return false;
      return oldData.every((item, index) => {
        if (typeof item === 'object' && item !== null) {
          return JSON.stringify(item) === JSON.stringify(newData[index]);
        }
        return item === newData[index];
      });
    }
    
    if (typeof oldData === 'object' && typeof newData === 'object') {
      return JSON.stringify(oldData) === JSON.stringify(newData);
    }
    
    return oldData === newData;
  }, []);

  const fetchData = useCallback(async (
    params: any,
    config: FetchConfig<T> = {}
  ): Promise<T | null> => {
    const { background = false, forceRefresh = false, compareData = isDataEqual } = config;
    
    const requestKey = createRequestKey(params);
    
    // Store the params for refresh operations
    lastParams.current = params;
    
    // Check if there's already an active request
    if (activeRequests.current.has(requestKey)) {
      return activeRequests.current.get(requestKey)!;
    }

    // Check cache first (unless force refresh)
    if (!forceRefresh) {
      const cachedData = getCachedData(params);
      if (cachedData) {
        setData(cachedData);
        setError(null);
        return cachedData;
      }
    }

    // Set loading state
    if (background) {
      setIsBackgroundLoading(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    const promise = fetchFunction(params)
      .then((newData) => {
        // Compare with current data to prevent unnecessary updates
        if (data && !forceRefresh) {
          const isEqual = compareData(data, newData);
          if (isEqual) {
            // Data is the same, don't update state
            return data;
          }
        }

        // Cache the new data
        setCachedData(params, newData);
        
        // Update state
        setData(newData);
        setError(null);
        
        config.onSuccess?.(newData);
        return newData;
      })
      .catch((err) => {
        const error = err instanceof Error ? err : new Error('An error occurred');
        setError(error);
        config.onError?.(error);
        throw error;
      })
      .finally(() => {
        if (background) {
          setIsBackgroundLoading(false);
        } else {
          setIsLoading(false);
        }
        activeRequests.current.delete(requestKey);
      });

    activeRequests.current.set(requestKey, promise);
    return promise;
  }, [fetchFunction, getCachedData, setCachedData, data, isDataEqual, createRequestKey]);

  const refreshData = useCallback(async (params?: any): Promise<T | null> => {
    const paramsToUse = params || lastParams.current;
    if (!paramsToUse) return null;
    
    return fetchData(paramsToUse, { forceRefresh: true });
  }, [fetchData]);

  const backgroundRefresh = useCallback(async (params?: any): Promise<T | null> => {
    const paramsToUse = params || lastParams.current;
    if (!paramsToUse) return null;
    
    return fetchData(paramsToUse, { background: true });
  }, [fetchData]);

  return {
    data,
    isLoading,
    isBackgroundLoading,
    error,
    fetchData,
    refreshData,
    backgroundRefresh,
    clearCache
  };
} 