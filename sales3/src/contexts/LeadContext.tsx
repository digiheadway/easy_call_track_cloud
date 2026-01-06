import React, { createContext, useContext, useState, ReactNode } from 'react'
import { Lead } from '../types'
import { dummyLeads } from '../data/dummyData'

interface LeadContextType {
  leads: Lead[]
  setLeads: (leads: Lead[]) => void
  addLead: (lead: Lead) => void
  updateLead: (id: number, lead: Partial<Lead>) => void
  deleteLead: (id: number) => void
  getLeadById: (id: number) => Lead | undefined
  searchLeads: (query: string) => Lead[]
  filterLeads: (filters: any) => Lead[]
}

const LeadContext = createContext<LeadContextType | undefined>(undefined)

export const useLeads = () => {
  const context = useContext(LeadContext)
  if (context === undefined) {
    throw new Error('useLeads must be used within a LeadProvider')
  }
  return context
}

interface LeadProviderProps {
  children: ReactNode
}

export const LeadProvider: React.FC<LeadProviderProps> = ({ children }) => {
  const [leads, setLeads] = useState<Lead[]>(dummyLeads)

  const addLead = (lead: Lead) => {
    setLeads(prev => [...prev, lead])
  }

  const updateLead = (id: number, updatedLead: Partial<Lead>) => {
    setLeads(prev => prev.map(lead => 
      lead.id === id ? { ...lead, ...updatedLead } : lead
    ))
  }

  const deleteLead = (id: number) => {
    setLeads(prev => prev.filter(lead => lead.id !== id))
  }

  const getLeadById = (id: number) => {
    return leads.find(lead => lead.id === id)
  }

  const searchLeads = (query: string) => {
    const lowercaseQuery = query.toLowerCase()
    return leads.filter(lead =>
      lead.name.toLowerCase().includes(lowercaseQuery) ||
      lead.phone.includes(query) ||
      lead.preferredLocation.toLowerCase().includes(lowercaseQuery) ||
      lead.requirementDescription.toLowerCase().includes(lowercaseQuery)
    )
  }

  const filterLeads = (filters: any) => {
    return leads.filter(lead => {
      if (filters.stage && filters.stage !== 'All Stages' && lead.stage !== filters.stage) return false
      if (filters.priority && filters.priority !== 'All Priorities' && lead.priority !== filters.priority) return false
      if (filters.source && filters.source !== 'All Sources' && lead.source !== filters.source) return false
      if (filters.segment && filters.segment !== 'All Segments' && lead.segment !== filters.segment) return false
      if (filters.purpose && filters.purpose !== 'All Purposes' && lead.purpose !== filters.purpose) return false
      if (filters.purchaseTimeline && filters.purchaseTimeline !== 'All Timelines' && lead.purchaseTimeline !== filters.purchaseTimeline) return false
      if (filters.minBudget && lead.budget < filters.minBudget) return false
      if (filters.maxBudget && lead.budget > filters.maxBudget) return false
      return true
    })
  }

  const value = {
    leads,
    setLeads,
    addLead,
    updateLead,
    deleteLead,
    getLeadById,
    searchLeads,
    filterLeads
  }

  return (
    <LeadContext.Provider value={value}>
      {children}
    </LeadContext.Provider>
  )
} 