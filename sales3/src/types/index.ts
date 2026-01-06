export interface Lead {
  id: number
  name: string
  phone: string
  alternateContact?: string
  address: string
  aboutPerson: string
  budget: number
  stage: string
  priority: string
  source: string
  segment: string
  purpose: string
  purchaseTimeline: string
  propertyType: string
  preferredLocation: string
  preferredSize: string
  requirementDescription: string
  interestedIn: string
  notInterestedIn: string
  intent: string
  tags: string[]
  assignedTo: string
  owner: string
  medium: string
  placement: string
  data1: string
  data2: string
  data3: string
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: number
  title: string
  description: string
  leadId: number
  leadName: string
  leadBudget: number
  dueDate: string
  status: 'today' | 'upcoming' | 'overdue' | 'closed'
  type: 'call' | 'meeting' | 'visit' | 'followup'
  assignedTo: string
  priority: 'low' | 'medium' | 'high'
  completed: boolean
  createdAt: string
  updatedAt: string
}

export interface Meeting {
  id: number
  title: string
  description: string
  leadId: number
  leadName: string
  date: string
  time: string
  location: string
  type: 'site-visit' | 'office-meeting' | 'phone-call'
  status: 'scheduled' | 'completed' | 'cancelled'
  assignedTo: string
  createdAt: string
}

export interface Activity {
  id: number
  type: 'call' | 'email' | 'meeting' | 'note'
  title: string
  description: string
  leadId: number
  leadName: string
  date: string
  assignedTo: string
  createdAt: string
} 