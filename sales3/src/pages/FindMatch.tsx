import React, { useState } from 'react'
import { Search, Filter, Plus, CheckCircle, X, RotateCcw } from 'lucide-react'
import { useTasks } from '../contexts/TaskContext'
import { format } from 'date-fns'

const FindMatch: React.FC = () => {
  const { tasks, getTasksByStatus, completeTask, deleteTask } = useTasks()
  const [activeTab, setActiveTab] = useState<'today' | 'upcoming' | 'overdue' | 'closed'>('overdue')
  const [searchQuery, setSearchQuery] = useState('')

  const filteredTasks = getTasksByStatus(activeTab).filter(task =>
    task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    task.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    task.leadName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-900">Find Match</h1>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search find match..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-64 rounded-lg border border-gray-300 pl-10 pr-4 py-2 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
              />
            </div>
            <button className="flex items-center gap-2 px-3 py-2 rounded-lg border bg-white border-gray-300 text-gray-700">
              <Filter size={16} />
              Filters
            </button>
          </div>
        </div>
        <button className="btn-primary flex items-center gap-2">
          <Plus size={16} />
          Add New Match
        </button>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {[
            { key: 'today', label: 'Today' },
            { key: 'upcoming', label: 'Upcoming' },
            { key: 'overdue', label: 'Overdue' },
            { key: 'closed', label: 'Closed' }
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.key
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tasks List */}
      <div className="space-y-4">
        {filteredTasks.map((task) => (
          <div key={task.id} className="bg-white rounded-lg border border-gray-200 p-6">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <h3 className="text-lg font-medium text-gray-900">
                    #{task.id} - {task.title}
                  </h3>
                </div>
                <p className="text-gray-600 mb-3">{task.description}</p>
                <div className="flex items-center gap-4 text-sm text-gray-500">
                  <span>{task.leadId}. {task.leadName} - {task.leadBudget}L</span>
                  <span>Due: {format(new Date(task.dueDate), 'dd MMM, h:mm a')}</span>
                </div>
                <div className="mt-4">
                  <button className="btn-primary">
                    Find Match
                  </button>
                </div>
              </div>
              <div className="flex items-center gap-2 ml-4">
                <button
                  onClick={() => completeTask(task.id)}
                  className="p-2 text-green-600 hover:text-green-700 hover:bg-green-50 rounded-lg"
                  title="Complete"
                >
                  <CheckCircle size={20} />
                </button>
                <button
                  onClick={() => deleteTask(task.id)}
                  className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg"
                  title="Delete"
                >
                  <X size={20} />
                </button>
                <button
                  className="p-2 text-gray-600 hover:text-gray-700 hover:bg-gray-50 rounded-lg"
                  title="Undo"
                >
                  <RotateCcw size={20} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default FindMatch 