import React from 'react';

interface LoadingIndicatorProps {
  type?: 'spinner' | 'dots' | 'pulse';
  size?: 'sm' | 'md' | 'lg';
  text?: string;
  className?: string;
}

const LoadingIndicator: React.FC<LoadingIndicatorProps> = ({
  type = 'spinner',
  size = 'md',
  text,
  className = ''
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8'
  };

  const renderSpinner = () => (
    <div className={`animate-spin rounded-full border-b-2 border-blue-600 ${sizeClasses[size]}`} />
  );

  const renderDots = () => (
    <div className="flex space-x-1">
      <div className={`animate-bounce rounded-full bg-blue-600 ${size === 'sm' ? 'h-2 w-2' : size === 'md' ? 'h-3 w-3' : 'h-4 w-4'}`} style={{ animationDelay: '0ms' }} />
      <div className={`animate-bounce rounded-full bg-blue-600 ${size === 'sm' ? 'h-2 w-2' : size === 'md' ? 'h-3 w-3' : 'h-4 w-4'}`} style={{ animationDelay: '150ms' }} />
      <div className={`animate-bounce rounded-full bg-blue-600 ${size === 'sm' ? 'h-2 w-2' : size === 'md' ? 'h-3 w-3' : 'h-4 w-4'}`} style={{ animationDelay: '300ms' }} />
    </div>
  );

  const renderPulse = () => (
    <div className={`animate-pulse rounded-full bg-blue-600 ${sizeClasses[size]}`} />
  );

  const renderLoader = () => {
    switch (type) {
      case 'dots':
        return renderDots();
      case 'pulse':
        return renderPulse();
      default:
        return renderSpinner();
    }
  };

  return (
    <div className={`flex items-center justify-center ${className}`}>
      {renderLoader()}
      {text && (
        <span className="ml-2 text-sm text-gray-600">{text}</span>
      )}
    </div>
  );
};

export default LoadingIndicator; 