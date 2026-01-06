
'use client';
import { useRef, useEffect } from 'react';

export function usePersistentScroll(key: string) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const scrollTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const scrollable = scrollRef.current;
    if (!scrollable) return;

    let initialPosition: number | null = null;
    try {
      const savedPosition = localStorage.getItem(key);
      if (savedPosition) {
        initialPosition = parseInt(savedPosition, 10);
      }
    } catch (error) {
        console.error(`Error reading scroll position for key "${key}":`, error);
    }
    
    // Defer setting scroll position until after the component has fully rendered
    const timeoutId = setTimeout(() => {
        if(scrollable && initialPosition !== null) {
            scrollable.scrollTop = initialPosition;
        }
    }, 0);


    const handleScroll = () => {
      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
      scrollTimeoutRef.current = setTimeout(() => {
        try {
            if(scrollable) {
                localStorage.setItem(key, scrollable.scrollTop.toString());
            }
        } catch (error) {
            console.error(`Error setting scroll position for key "${key}":`, error);
        }
      }, 150);
    };

    scrollable.addEventListener('scroll', handleScroll, { passive: true });

    return () => {
      clearTimeout(timeoutId);
      if (scrollable) {
          scrollable.removeEventListener('scroll', handleScroll);
      }
      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
    };
  }, [key]);

  return scrollRef;
}

    