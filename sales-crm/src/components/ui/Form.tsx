import React from 'react';
import classNames from 'classnames';

interface FormProps extends React.FormHTMLAttributes<HTMLFormElement> {
  children: React.ReactNode;
}

const Form: React.FC<FormProps> = ({ children, className, ...props }) => {
  return (
    <form className={classNames('space-y-4', className)} {...props}>
      {children}
    </form>
  );
};

export default Form;