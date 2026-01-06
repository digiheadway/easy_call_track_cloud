import React, { useState } from 'react'
import { ChevronLeft, ChevronRight, Phone, Users, CheckSquare } from 'lucide-react'
import { useTasks } from '../contexts/TaskContext'
import { format, startOfWeek, addDays, isSameDay, parseISO } from 'date-fns'

const Calendar: React.FC = () => {
  const { tasks, getTasksByStatus } = useTasks()
  const [currentDate, setCurrentDate] = useState(new Date())
  const [showClosedTasks, setShowClosedTasks] = useState(false)
  const [viewMode, setViewMode] = useState<'week' | 'month'>('week')

  const weekStart = startOfWeek(currentDate, { weekStartsOn: 0 })
  const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i))

  const todayTasks = getTasksByStatus('today')
  const selectedDate = currentDate

  const getTasksForDate = (date: Date) => {
    return todayTasks.filter(task => {
      const taskDate = parseISO(task.dueDate)
      return isSameDay(taskDate, date)
    })
  }

  const goToPreviousWeek = () => {
    setCurrentDate(prev => addDays(prev, -7))
  }

  const goToNextWeek = () => {
    setCurrentDate(prev => addDays(prev, 7))
  }

  const goToToday = () => {
    setCurrentDate(new Date())
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Calendar</h1>
      </div>

      {/* Calendar Controls */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <h2 className="text-lg font-semibold text-gray-900">
              Week {format(currentDate, 'w')} - {format(currentDate, 'MMMM yyyy')}
            </h2>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={showClosedTasks}
                onChange={(e) => setShowClosedTasks(e.target.checked)}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              <span className="text-sm text-gray-700">Show closed tasks</span>
            </label>
          </div>
          
          <div className="flex items-center gap-2">
            <div className="flex rounded-lg border border-gray-300">
              <button
                onClick={() => setViewMode('week')}
                className={`px-3 py-1 text-sm font-medium rounded-l-lg ${
                  viewMode === 'week'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                W
              </button>
              <button
                onClick={() => setViewMode('month')}
                className={`px-3 py-1 text-sm font-medium rounded-r-lg ${
                  viewMode === 'month'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                M
              </button>
            </div>
            
            <div className="flex items-center gap-1">
              <button
                onClick={goToPreviousWeek}
                className="p-1 text-gray-400 hover:text-gray-600"
              >
                <ChevronLeft size={20} />
              </button>
              <button
                onClick={goToNextWeek}
                className="p-1 text-gray-400 hover:text-gray-600"
              >
                <ChevronRight size={20} />
              </button>
              <button
                onClick={goToToday}
                className="ml-2 px-3 py-1 text-sm font-medium text-primary-600 hover:text-primary-700"
              >
                Today
              </button>
            </div>
            
            <div className="flex items-center gap-1">
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <Phone size={16} />
              </button>
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <Users size={16} />
              </button>
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <CheckSquare size={16} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <div className="grid grid-cols-7 gap-px bg-gray-200">
          {/* Header */}
          {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
            <div key={day} className="bg-gray-50 p-4 text-center">
              <div className="text-sm font-medium text-gray-900">{day}</div>
            </div>
          ))}
          
          {/* Days */}
          {weekDays.map((date, index) => {
            const dayTasks = getTasksForDate(date)
            const isSelected = isSameDay(date, selectedDate)
            
            return (
              <div
                key={index}
                className={`min-h-32 bg-white p-4 ${
                  isSelected ? 'ring-2 ring-primary-500' : ''
                }`}
              >
                <div className="text-sm font-medium text-gray-900 mb-2">
                  {format(date, 'd')}
                </div>
                
                {dayTasks.map((task) => (
                  <div key={task.id} className="mb-2 p-2 bg-green-50 rounded border border-green-200">
                    <div className="text-xs text-gray-600">
                      {format(parseISO(task.dueDate), 'h:mm a')} - #{task.id}
                    </div>
                    <div className="text-sm text-gray-900">{task.description}</div>
                    <button className="mt-1 w-full text-xs bg-green-600 text-white px-2 py-1 rounded hover:bg-green-700">
                      Schedule Site Visit
                    </button>
                  </div>
                ))}
              </div>
            )
          })}
        </div>
      </div>

      {/* Tasks for Selected Date */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Tasks for {format(selectedDate, 'MMMM d, yyyy')}
        </h3>
        
        {getTasksForDate(selectedDate).map((task) => (
          <div key={task.id} className="flex items-start gap-4 p-4 border border-gray-200 rounded-lg mb-4">
            <button className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700">
              Schedule Site Visit
            </button>
            <div className="flex-1">
              <h4 className="font-medium text-gray-900">{task.description}</h4>
              <p className="text-sm text-gray-600">{task.leadName} - {task.leadBudget}L</p>
              <div className="text-xs text-gray-500 mt-1">
                {format(parseISO(task.dueDate), 'h:mm a')} #{task.id}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button className="p-1 text-green-600 hover:text-green-700">
                <CheckSquare size={16} />
              </button>
              <button className="p-1 text-red-600 hover:text-red-700">
                <span className="text-xs">✗</span>
              </button>
            </div>
          </div>
        ))}
        
        {getTasksForDate(selectedDate).length === 0 && (
          <p className="text-gray-500 text-center py-8">No tasks for this date</p>
        )}
      </div>
    </div>
  )
}

export default Calendar 