import React, { createContext, useContext, useState, ReactNode } from 'react'
import { Task } from '../types'
import { dummyTasks } from '../data/dummyData'

interface TaskContextType {
  tasks: Task[]
  setTasks: (tasks: Task[]) => void
  addTask: (task: Task) => void
  updateTask: (id: number, task: Partial<Task>) => void
  deleteTask: (id: number) => void
  getTaskById: (id: number) => Task | undefined
  getTasksByStatus: (status: Task['status']) => Task[]
  getTasksByLead: (leadId: number) => Task[]
  completeTask: (id: number) => void
}

const TaskContext = createContext<TaskContextType | undefined>(undefined)

export const useTasks = () => {
  const context = useContext(TaskContext)
  if (context === undefined) {
    throw new Error('useTasks must be used within a TaskProvider')
  }
  return context
}

interface TaskProviderProps {
  children: ReactNode
}

export const TaskProvider: React.FC<TaskProviderProps> = ({ children }) => {
  const [tasks, setTasks] = useState<Task[]>(dummyTasks)

  const addTask = (task: Task) => {
    setTasks(prev => [...prev, task])
  }

  const updateTask = (id: number, updatedTask: Partial<Task>) => {
    setTasks(prev => prev.map(task => 
      task.id === id ? { ...task, ...updatedTask } : task
    ))
  }

  const deleteTask = (id: number) => {
    setTasks(prev => prev.filter(task => task.id !== id))
  }

  const getTaskById = (id: number) => {
    return tasks.find(task => task.id === id)
  }

  const getTasksByStatus = (status: Task['status']) => {
    return tasks.filter(task => task.status === status)
  }

  const getTasksByLead = (leadId: number) => {
    return tasks.filter(task => task.leadId === leadId)
  }

  const completeTask = (id: number) => {
    setTasks(prev => prev.map(task => 
      task.id === id ? { ...task, completed: true, status: 'closed' as const } : task
    ))
  }

  const value = {
    tasks,
    setTasks,
    addTask,
    updateTask,
    deleteTask,
    getTaskById,
    getTasksByStatus,
    getTasksByLead,
    completeTask
  }

  return (
    <TaskContext.Provider value={value}>
      {children}
    </TaskContext.Provider>
  )
} 