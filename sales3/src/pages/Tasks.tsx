import React, { useState } from 'react'
import { Search, Filter, Plus, CheckCircle, X, RotateCcw } from 'lucide-react'
import { useTasks } from '../contexts/TaskContext'
import { format } from 'date-fns'

const Tasks: React.FC = () => {
  const { tasks, getTasksByStatus, completeTask, deleteTask } = useTasks()
  const [activeTab, setActiveTab] = useState<'today' | 'upcoming' | 'overdue' | 'closed'>('today')
  const [searchQuery, setSearchQuery] = useState('')

  const filteredTasks = getTasksByStatus(activeTab).filter(task =>
    task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    task.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    task.leadName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleComplete = (taskId: number) => {
    completeTask(taskId)
  }

  const handleDelete = (taskId: number) => {
    deleteTask(taskId)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-900">Tasks</h1>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search tasks..."
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
          Add New Task
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
        {filteredTasks.length === 0 ? (
          <div className="text-center py-12">
            <div className="mx-auto h-12 w-12 text-gray-400">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No tasks found</h3>
            <p className="mt-1 text-sm text-gray-500">
              No {activeTab} tasks found
            </p>
          </div>
        ) : (
          filteredTasks.map((task) => (
            <div key={task.id} className="bg-white rounded-lg border border-gray-200 p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="text-lg font-medium text-gray-900">
                      #{task.id} - {task.title}
                    </h3>
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      task.priority === 'high' ? 'bg-red-100 text-red-800' :
                      task.priority === 'medium' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-green-100 text-green-800'
                    }`}>
                      {task.priority}
                    </span>
                  </div>
                  <p className="text-gray-600 mb-3">{task.description}</p>
                  <div className="flex items-center gap-4 text-sm text-gray-500">
                    <span>{task.leadId}. {task.leadName} - {task.leadBudget}L</span>
                    <span>Due: {format(new Date(task.dueDate), 'dd MMM, h:mm a')}</span>
                    <span>Assigned to: {task.assignedTo}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2 ml-4">
                  <button
                    onClick={() => handleComplete(task.id)}
                    className="p-2 text-green-600 hover:text-green-700 hover:bg-green-50 rounded-lg"
                    title="Complete"
                  >
                    <CheckCircle size={20} />
                  </button>
                  <button
                    onClick={() => handleDelete(task.id)}
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
          ))
        )}
      </div>
    </div>
  )
}

export default Tasks 