import React from 'react';
import { X } from 'lucide-react';
import classNames from 'classnames';

interface BadgeProps {
  label: string;
  color: 'blue' | 'gray' | 'green' | 'red' | 'yellow' | 'purple' | 'orange';
  onClose?: () => void;
  small?: boolean;
  className?: string;
}

const Badge: React.FC<BadgeProps> = ({
  label,
  color,
  onClose,
  small = false,
  className,
}) => {
  const colorClasses = {
    blue: 'bg-blue-100 text-blue-800',
    gray: 'bg-gray-100 text-gray-800',
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    purple: 'bg-purple-100 text-purple-800',
    orange: 'bg-orange-100 text-orange-800',
  };

  return (
    <span
      className={classNames(
        'inline-flex items-center rounded-full',
        colorClasses[color],
        small ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-0.5 text-sm',
        className
      )}
    >
      {label}
      {onClose && (
        <button
          type="button"
          className="ml-1.5 h-3.5 w-3.5 rounded-full flex items-center justify-center hover:bg-gray-200 hover:bg-opacity-30 focus:outline-none"
          onClick={onClose}
        >
          <X size={10} />
        </button>
      )}
    </span>
  );
};

export default Badge;