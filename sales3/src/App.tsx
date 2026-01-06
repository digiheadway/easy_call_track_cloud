import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { useState } from 'react'
import Layout from './components/Layout'
import Leads from './pages/Leads'
import LeadDetails from './pages/LeadDetails'
import Tasks from './pages/Tasks'
import FindMatch from './pages/FindMatch'
import FollowUps from './pages/FollowUps'
import ScheduleVisit from './pages/ScheduleVisit'
import Meetings from './pages/Meetings'
import Activities from './pages/Activities'
import Calendar from './pages/Calendar'
import { LeadProvider } from './contexts/LeadContext'
import { TaskProvider } from './contexts/TaskContext'

function App() {
  return (
    <LeadProvider>
      <TaskProvider>
        <Router>
          <div className="min-h-screen bg-gray-50">
            <Layout>
              <Routes>
                <Route path="/" element={<Leads />} />
                <Route path="/leads" element={<Leads />} />
                <Route path="/leads/:id" element={<LeadDetails />} />
                <Route path="/tasks" element={<Tasks />} />
                <Route path="/find-match" element={<FindMatch />} />
                <Route path="/follow-ups" element={<FollowUps />} />
                <Route path="/schedule-visit" element={<ScheduleVisit />} />
                <Route path="/meetings" element={<Meetings />} />
                <Route path="/activities" element={<Activities />} />
                <Route path="/calendar" element={<Calendar />} />
              </Routes>
            </Layout>
          </div>
        </Router>
      </TaskProvider>
    </LeadProvider>
  )
}

export default App 