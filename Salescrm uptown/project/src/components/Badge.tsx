import React from 'react';

interface BadgeProps {
  text: string;
  onRemove?: () => void;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ text, onRemove, className = '' }) => {
  return (
    <span 
      className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800 ${className}`}
    >
      {text}
      {onRemove && (
        <button
          onClick={onRemove}
          className="ml-1.5 hover:bg-gray-200 rounded-full p-0.5 transition-colors"
        >
          ×
        </button>
      )}
    </span>
  );
}