import React from 'react';
import { useAppContext } from '../../contexts/AppContext';
import { BarChart3, Users, CheckSquare, TrendingUp } from 'lucide-react';

const Dashboard: React.FC = () => {
  const { leads, todos } = useAppContext();

  const stats = [
    {
      name: 'Total Leads',
      value: leads.length,
      icon: Users,
      change: '+4.75%',
      changeType: 'positive',
    },
    {
      name: 'Active Tasks',
      value: todos.filter(todo => todo.status === 'Pending').length,
      icon: CheckSquare,
      change: '+54.02%',
      changeType: 'positive',
    },
    {
      name: 'Conversion Rate',
      value: '24.57%',
      icon: TrendingUp,
      change: '+6.18%',
      changeType: 'positive',
    },
    {
      name: 'Revenue',
      value: '₹32.5L',
      icon: BarChart3,
      change: '+11.25%',
      changeType: 'positive',
    },
  ];

  return (
    <div className="p-6">
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <div
            key={stat.name}
            className="relative overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:px-6 sm:py-6"
          >
            <dt>
              <div className="absolute rounded-md bg-blue-800 p-3">
                <stat.icon className="h-6 w-6 text-white" aria-hidden="true" />
              </div>
              <p className="ml-16 truncate text-sm font-medium text-gray-500">
                {stat.name}
              </p>
            </dt>
            <dd className="ml-16 flex items-baseline">
              <p className="text-2xl font-semibold text-gray-900">{stat.value}</p>
              <p
                className={`ml-2 flex items-baseline text-sm font-semibold ${
                  stat.changeType === 'positive' ? 'text-green-600' : 'text-red-600'
                }`}
              >
                {stat.change}
              </p>
            </dd>
          </div>
        ))}
      </div>

      {/* Add more dashboard components here */}
    </div>
  );
};

export default Dashboard;