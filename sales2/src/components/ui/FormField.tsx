import React from 'react';
import classNames from 'classnames';

interface FormFieldProps {
  label: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
  helpText?: string;
}

const FormField: React.FC<FormFieldProps> = ({
  label,
  error,
  required,
  className,
  children,
  helpText,
}) => {
  return (
    <div className={classNames('space-y-1', className)}>
      <label className="block text-sm font-medium text-gray-700">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>
      {children}
      {helpText && (
        <p className="text-sm text-gray-500">{helpText}</p>
      )}
      {error && (
        <p className="text-sm text-red-600">{error}</p>
      )}
    </div>
  );
};

export default FormField;