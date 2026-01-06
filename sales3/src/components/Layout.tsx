import React, { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { 
  Users, 
  CheckSquare, 
  Search, 
  Phone, 
  Building, 
  Calendar,
  Activity,
  Menu,
  X,
  User
} from 'lucide-react'

interface LayoutProps {
  children: React.ReactNode
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()

  const navigation = [
    { name: 'Leads', href: '/leads', icon: Users },
    { name: 'Tasks', href: '/tasks', icon: CheckSquare },
    { name: 'Find Match', href: '/find-match', icon: Search },
    { name: 'Follow-ups', href: '/follow-ups', icon: Phone },
    { name: 'To Schedule Visit', href: '/schedule-visit', icon: Building },
    { name: 'My Meetings', href: '/meetings', icon: Users },
    { name: 'Activities', href: '/activities', icon: Activity },
    { name: 'Calendar', href: '/calendar', icon: Calendar },
  ]

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile sidebar */}
      <div className={`fixed inset-0 z-50 lg:hidden ${sidebarOpen ? 'block' : 'hidden'}`}>
        <div className="fixed inset-0 bg-gray-600 bg-opacity-75" onClick={() => setSidebarOpen(false)} />
        <div className="fixed inset-y-0 left-0 flex w-64 flex-col bg-gray-800">
          <div className="flex h-16 items-center justify-between px-4">
            <h1 className="text-xl font-semibold text-white">Uptown Properties</h1>
            <button
              onClick={() => setSidebarOpen(false)}
              className="text-gray-300 hover:text-white"
            >
              <X size={24} />
            </button>
          </div>
          <nav className="flex-1 space-y-1 px-2 py-4">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`sidebar-item ${isActive ? 'active' : ''}`}
                  onClick={() => setSidebarOpen(false)}
                >
                  <item.icon size={20} />
                  {item.name}
                </Link>
              )
            })}
          </nav>
          <div className="flex items-center gap-2 px-4 py-4 text-sm text-gray-400">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-white">
              <User size={16} />
            </div>
            <div>
              <div>Real Estate CRM</div>
              <div>v1.0.0</div>
            </div>
          </div>
        </div>
      </div>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
        <div className="flex flex-col flex-grow bg-gray-800">
          <div className="flex h-16 items-center px-4">
            <h1 className="text-xl font-semibold text-white">Uptown Properties</h1>
          </div>
          <nav className="flex-1 space-y-1 px-2 py-4">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`sidebar-item ${isActive ? 'active' : ''}`}
                >
                  <item.icon size={20} />
                  {item.name}
                </Link>
              )
            })}
          </nav>
          <div className="flex items-center gap-2 px-4 py-4 text-sm text-gray-400">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-white">
              <User size={16} />
            </div>
            <div>
              <div>Real Estate CRM</div>
              <div>v1.0.0</div>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Top header */}
        <div className="sticky top-0 z-40 flex h-16 items-center gap-x-4 border-b border-gray-200 bg-white px-4 shadow-sm sm:gap-x-6 sm:px-6 lg:px-8">
          <button
            type="button"
            className="-m-2.5 p-2.5 text-gray-700 lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu size={24} />
          </button>
          
          <div className="flex flex-1 items-center justify-between">
            <div className="flex items-center gap-x-4">
              <h1 className="text-lg font-semibold text-gray-900">
                {navigation.find(item => item.href === location.pathname)?.name || 'Dashboard'}
              </h1>
            </div>
            
            <div className="flex items-center gap-x-4">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-white">
                <User size={16} />
              </div>
            </div>
          </div>
        </div>

        {/* Page content */}
        <main className="py-6">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {children}
          </div>
        </main>
      </div>
    </div>
  )
}

export default Layout 