import React from 'react';
import classNames from 'classnames';

interface FormActionsProps {
  children: React.ReactNode;
  align?: 'left' | 'center' | 'right';
  className?: string;
}

const FormActions: React.FC<FormActionsProps> = ({
  children,
  align = 'right',
  className,
}) => {
  return (
    <div
      className={classNames(
        'flex gap-3 mt-6',
        {
          'justify-start': align === 'left',
          'justify-center': align === 'center',
          'justify-end': align === 'right',
        },
        className
      )}
    >
      {children}
    </div>
  );
};

export default FormActions;