import React from 'react';

interface ShimmerProps {
  className?: string;
}

export const Shimmer: React.FC<ShimmerProps> = ({ className = '' }) => {
  return (
    <div className={`animate-shimmer bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:400%_100%] ${className}`}>
      &nbsp;
    </div>
  );
};

export const LeadCardShimmer: React.FC = () => {
  return (
    <div className="bg-white rounded-lg shadow-sm border p-4">
      <div className="flex justify-between items-start mb-4">
        <div className="space-y-2">
          <Shimmer className="h-5 w-32 rounded" />
          <Shimmer className="h-4 w-24 rounded" />
        </div>
        <div className="text-right">
          <Shimmer className="h-5 w-20 rounded" />
          <Shimmer className="h-6 w-16 rounded-full mt-1" />
        </div>
      </div>
      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <Shimmer className="h-4 w-4 rounded" />
          <Shimmer className="h-4 flex-1 rounded" />
        </div>
        <div className="flex items-center gap-2">
          <Shimmer className="h-4 w-4 rounded" />
          <Shimmer className="h-4 flex-1 rounded" />
        </div>
        <div className="flex items-center gap-2">
          <Shimmer className="h-4 w-4 rounded" />
          <Shimmer className="h-4 flex-1 rounded" />
        </div>
        <div className="flex items-start gap-2 mt-2">
          <Shimmer className="h-4 w-4 rounded" />
          <Shimmer className="h-10 flex-1 rounded" />
        </div>
      </div>
    </div>
  );
};