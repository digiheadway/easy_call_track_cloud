import React, { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Phone, MessageSquare, Plus, Share2, ChevronDown, ChevronUp, X } from 'lucide-react'
import { useLeads } from '../contexts/LeadContext'
import { format } from 'date-fns'

const LeadDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const { getLeadById, updateLead } = useLeads()
  const lead = getLeadById(Number(id))
  
  const [contactCollapsed, setContactCollapsed] = useState(false)
  const [additionalDataCollapsed, setAdditionalDataCollapsed] = useState(false)

  if (!lead) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-gray-900">Lead not found</h2>
          <p className="text-gray-600">The lead you're looking for doesn't exist.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Main Content */}
      <div className="lg:col-span-2 space-y-6">
        {/* Header */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                #{lead.id} {lead.name} - {lead.budget} Lakh
              </h1>
              <p className="text-gray-600 mt-1">
                {lead.preferredLocation} -- {format(new Date(lead.createdAt), 'dd MMM yyyy, h:mm a')}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <Phone size={20} />
              </button>
              <button className="p-2 text-green-500 hover:text-green-600">
                <MessageSquare size={20} />
              </button>
              <button className="btn-primary flex items-center gap-2">
                <Plus size={16} />
                Add Todo
              </button>
              <button className="btn-secondary flex items-center gap-2">
                <Plus size={16} />
                Add Activity
              </button>
              <button className="p-2 text-gray-400 hover:text-gray-600">
                <Share2 size={20} />
              </button>
            </div>
          </div>
        </div>

        {/* Stage & Priority */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Stage</label>
              <select
                value={lead.stage}
                onChange={(e) => updateLead(lead.id, { stage: e.target.value })}
                className="input-field"
              >
                <option>Mid - Waiting for Match</option>
                <option>New</option>
                <option>Qualified</option>
                <option>Closed</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Priority</label>
              <select
                value={lead.priority}
                onChange={(e) => updateLead(lead.id, { priority: e.target.value })}
                className="input-field"
              >
                <option>General</option>
                <option>High</option>
                <option>Medium</option>
                <option>Low</option>
              </select>
            </div>
          </div>
        </div>

        {/* Property Requirements */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Property Requirements</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Property Type</label>
              <select
                value={lead.propertyType}
                onChange={(e) => updateLead(lead.id, { propertyType: e.target.value })}
                className="input-field"
              >
                <option value="">Add property types</option>
                <option>House</option>
                <option>Plot</option>
                <option>Shop</option>
                <option>Office</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Budget (in Lakhs)</label>
              <input
                type="number"
                value={lead.budget}
                onChange={(e) => updateLead(lead.id, { budget: Number(e.target.value) })}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Requirement Description</label>
              <textarea
                value={lead.requirementDescription}
                onChange={(e) => updateLead(lead.id, { requirementDescription: e.target.value })}
                rows={4}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Preferred Location</label>
              <select
                value={lead.preferredLocation}
                onChange={(e) => updateLead(lead.id, { preferredLocation: e.target.value })}
                className="input-field"
              >
                <option value="">Add locations</option>
                <option>Panipat</option>
                <option>Delhi</option>
                <option>Any</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Preferred Size</label>
              <select
                value={lead.preferredSize}
                onChange={(e) => updateLead(lead.id, { preferredSize: e.target.value })}
                className="input-field"
              >
                <option value="">Add sizes</option>
                <option>100-150 Gaj</option>
                <option>150-200 Gaj</option>
                <option>200-300 Gaj</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Purpose</label>
              <select
                value={lead.purpose}
                onChange={(e) => updateLead(lead.id, { purpose: e.target.value })}
                className="input-field"
              >
                <option>Self Use</option>
                <option>Investment</option>
                <option>Other</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Purchase Timeline</label>
              <select
                value={lead.purchaseTimeline}
                onChange={(e) => updateLead(lead.id, { purchaseTimeline: e.target.value })}
                className="input-field"
              >
                <option>ASAP</option>
                <option>Within 3 months</option>
                <option>Within 6 months</option>
                <option>Within 4 months</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Interested In</label>
              <input
                type="text"
                value={lead.interestedIn}
                onChange={(e) => updateLead(lead.id, { interestedIn: e.target.value })}
                className="input-field"
              />
            </div>
          </div>
        </div>

        {/* Contact Information */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Contact Information</h2>
            <button
              onClick={() => setContactCollapsed(!contactCollapsed)}
              className="text-gray-400 hover:text-gray-600"
            >
              {contactCollapsed ? <ChevronDown size={20} /> : <ChevronUp size={20} />}
            </button>
          </div>
          {!contactCollapsed && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Name</label>
                <input
                  type="text"
                  value={lead.name}
                  onChange={(e) => updateLead(lead.id, { name: e.target.value })}
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Phone</label>
                <input
                  type="tel"
                  value={lead.phone}
                  onChange={(e) => updateLead(lead.id, { phone: e.target.value })}
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Alternate Contact</label>
                <input
                  type="text"
                  value={lead.alternateContact}
                  onChange={(e) => updateLead(lead.id, { alternateContact: e.target.value })}
                  placeholder="Additional contact details"
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Address</label>
                <input
                  type="text"
                  value={lead.address}
                  onChange={(e) => updateLead(lead.id, { address: e.target.value })}
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">About Person</label>
                <textarea
                  value={lead.aboutPerson}
                  onChange={(e) => updateLead(lead.id, { aboutPerson: e.target.value })}
                  rows={3}
                  className="input-field"
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Right Sidebar */}
      <div className="space-y-6">
        {/* Lead Status */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Lead Status</h2>
            <button className="text-gray-400 hover:text-gray-600">
              <X size={20} />
            </button>
          </div>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Tags</label>
              <select className="input-field">
                <option>Add tags</option>
                <option>VIP</option>
                <option>Investment</option>
                <option>First Time Buyer</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Segment</label>
              <select
                value={lead.segment}
                onChange={(e) => updateLead(lead.id, { segment: e.target.value })}
                className="input-field"
              >
                <option>Panipat</option>
                <option>Delhi</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Note</label>
              <textarea
                placeholder="Enter general notes"
                rows={3}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Not Interested In</label>
              <input
                type="text"
                value={lead.notInterestedIn}
                onChange={(e) => updateLead(lead.id, { notInterestedIn: e.target.value })}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Intent</label>
              <select className="input-field">
                <option>Select Intent</option>
                <option>Buy</option>
                <option>Rent</option>
                <option>Invest</option>
              </select>
            </div>
          </div>
        </div>

        {/* Source Information */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Source Information</h2>
            <button
              onClick={() => setAdditionalDataCollapsed(!additionalDataCollapsed)}
              className="text-gray-400 hover:text-gray-600"
            >
              {additionalDataCollapsed ? <ChevronDown size={20} /> : <ChevronUp size={20} />}
            </button>
          </div>
          {!additionalDataCollapsed && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Source</label>
                <select
                  value={lead.source}
                  onChange={(e) => updateLead(lead.id, { source: e.target.value })}
                  className="input-field"
                >
                  <option>Other</option>
                  <option>Website</option>
                  <option>Referral</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Assigned To</label>
                <select className="input-field">
                  <option>Assign team members</option>
                  <option>John Doe</option>
                  <option>Jane Smith</option>
                  <option>Mike Johnson</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Medium</label>
                <select className="input-field">
                  <option>Select medium</option>
                  <option>Online</option>
                  <option>Referral</option>
                  <option>Other</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Placement</label>
                <select className="input-field">
                  <option>Select placement</option>
                  <option>Website</option>
                  <option>Word of Mouth</option>
                  <option>Other</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Data 1</label>
                <input type="text" className="input-field" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Data 2</label>
                <input type="text" className="input-field" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Data 3</label>
                <input type="text" className="input-field" />
              </div>
            </div>
          )}
        </div>

        {/* Timestamps */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="text-sm text-gray-600">
            <div>Created: {format(new Date(lead.createdAt), 'dd MMM yyyy, h:mm a')}</div>
            <div>Updated: {format(new Date(lead.updatedAt), 'dd MMM yyyy, h:mm a')}</div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LeadDetails 