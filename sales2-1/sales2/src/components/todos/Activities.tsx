import React from 'react';
import TodosList from './TodosList';

const Activities: React.FC = () => {
  // Don't call fetchTodos here - let TodosList handle it
  return (
    <TodosList 
      defaultType="Activity"
      title="Activities"
      showAddButton={false}
      isActivityView={true}
    />
  );
};

export default Activities;