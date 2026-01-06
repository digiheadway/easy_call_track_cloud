import { useState, useCallback } from 'react';
import { toast } from 'react-toastify';

export interface ValidationError {
  message: string;
  path: string[];
}

interface FormConfig<T> {
  initialValues: T;
  onSubmit: (values: T) => Promise<void>;
  validate?: (values: T) => ValidationError[] | Promise<ValidationError[]>;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}

interface FormState<T> {
  values: T;
  errors: Record<string, string>;
  touched: Record<string, boolean>;
  isSubmitting: boolean;
  isValid: boolean;
}

export function useForm<T extends Record<string, any>>({
  initialValues,
  onSubmit,
  validate,
  onSuccess,
  onError,
}: FormConfig<T>) {
  const [formState, setFormState] = useState<FormState<T>>({
    values: initialValues,
    errors: {},
    touched: {},
    isSubmitting: false,
    isValid: true,
  });

  const handleChange = useCallback((field: keyof T, value: any) => {
    setFormState((prev) => ({
      ...prev,
      values: { ...prev.values, [field]: value },
      touched: { ...prev.touched, [field]: true },
    }));
  }, []);

  const setFieldTouched = useCallback((field: keyof T, isTouched = true) => {
    setFormState((prev) => ({
      ...prev,
      touched: { ...prev.touched, [field]: isTouched },
    }));
  }, []);

  const validateForm = useCallback(async () => {
    if (!validate) return true;

    try {
      const validationErrors = await validate(formState.values);
      const errors: Record<string, string> = {};

      validationErrors.forEach((error) => {
        const field = error.path.join('.');
        errors[field] = error.message;
      });

      setFormState((prev) => ({
        ...prev,
        errors,
        isValid: validationErrors.length === 0,
      }));

      return validationErrors.length === 0;
    } catch (error) {
      console.error('Validation error:', error);
      return false;
    }
  }, [formState.values, validate]);

  const handleSubmit = useCallback(
    async (e?: React.FormEvent) => {
      if (e) {
        e.preventDefault();
      }

      setFormState((prev) => ({ ...prev, isSubmitting: true }));

      try {
        const isValid = await validateForm();
        if (!isValid) {
          throw new Error('Form validation failed');
        }

        await onSubmit(formState.values);
        onSuccess?.();
        toast.success('Form submitted successfully');
      } catch (error) {
        const errorMessage =
          error instanceof Error ? error.message : 'An error occurred';
        toast.error(errorMessage);
        onError?.(error instanceof Error ? error : new Error(errorMessage));
      } finally {
        setFormState((prev) => ({ ...prev, isSubmitting: false }));
      }
    },
    [formState.values, onSubmit, validateForm, onSuccess, onError]
  );

  const reset = useCallback(() => {
    setFormState({
      values: initialValues,
      errors: {},
      touched: {},
      isSubmitting: false,
      isValid: true,
    });
  }, [initialValues]);

  return {
    values: formState.values,
    errors: formState.errors,
    touched: formState.touched,
    isSubmitting: formState.isSubmitting,
    isValid: formState.isValid,
    handleChange,
    setFieldTouched,
    handleSubmit,
    reset,
  };
}