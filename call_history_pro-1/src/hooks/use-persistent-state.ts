
'use client';

import { useState, useEffect, useCallback } from 'react';

type Parser<T> = (val: any) => T;

export function usePersistentState<T>(key: string, defaultValue: T, parser?: Parser<T>): [T, React.Dispatch<React.SetStateAction<T>>] {
  const [state, setState] = useState<T>(() => {
    if (typeof window === 'undefined') {
      return defaultValue;
    }
    try {
      const storedValue = window.localStorage.getItem(key);
      if (storedValue && storedValue !== 'undefined' && storedValue !== 'null') {
        const parsed = JSON.parse(storedValue);
        return parser ? parser(parsed) : parsed;
      }
    } catch (error) {
      console.error(`Error reading localStorage key “${key}”:`, error);
    }
    return defaultValue;
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(state));
    } catch (error) {
      console.error(`Error setting localStorage key “${key}”:`, error);
    }
  }, [key, state]);

  return [state, setState];
}

    