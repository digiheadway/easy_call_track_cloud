import { useState } from 'react';
import { toast } from 'react-toastify';

interface ApiConfig<T> {
  onSuccess?: (data: T) => void;
  onError?: (error: Error) => void;
  successMessage?: string;
}

export function useApi<T>() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const execute = async (
    apiCall: () => Promise<T>,
    config: ApiConfig<T> = {}
  ) => {
    const { onSuccess, onError, successMessage } = config;
    setIsLoading(true);
    setError(null);

    try {
      const result = await apiCall();
      if (successMessage) {
        toast.success(successMessage);
      }
      onSuccess?.(result);
      return result;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('An error occurred');
      setError(error);
      toast.error(error.message);
      onError?.(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    execute,
    isLoading,
    error,
  };
}