import React from 'react';

const LoadingBar: React.FC = () => {
  return (
    <div className="fixed top-0 left-0 right-0 z-50">
      <div className="h-1 w-full bg-blue-100">
        <div className="h-1 bg-blue-800 w-0 animate-[loading_1s_ease-in-out_infinite]"></div>
      </div>
    </div>
  );
};

export default LoadingBar;