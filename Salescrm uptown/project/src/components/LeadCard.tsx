import React from 'react';
import { formatDateTime } from '../utils/dateFormat';
import { Lead } from '../types/lead';
import { 
  IndianRupee, 
  Clock,
  Tag,
  MessageSquare,
  Target,
  BarChart3,
  CircleDot,
  Building,
  Coins
} from 'lucide-react';
import { format, isToday, isTomorrow, isYesterday } from 'date-fns';

interface LeadCardProps {
  lead: Lead;
  onClick: () => void;
}

export const LeadCard: React.FC<LeadCardProps> = ({ lead, onClick }) => {
  const getTimeColor = (dateString: string | null): string => {
    if (!dateString) return 'text-gray-500';
    
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return 'text-gray-500';
    
    if (isToday(date)) {
      return 'text-emerald-600';
    }
    
    if (isTomorrow(date)) {
      return 'text-blue-600';
    }

    if (date < new Date()) {
      return 'text-red-600';
    }
    
    return 'text-gray-600';
  };

  const getPriorityColor = (priority: number | null): string => {
    if (!priority) return 'text-gray-500';
    switch (priority) {
      case 3: return 'text-red-600';
      case 2: return 'text-orange-600';
      case 1: return 'text-blue-600';
      default: return 'text-gray-500';
    }
  };

  const getPriorityLabel = (priority: number | null): string => {
    if (!priority) return 'No Priority';
    switch (priority) {
      case 3: return 'High';
      case 2: return 'Medium';
      case 1: return 'Low';
      default: return 'No Priority';
    }
  };

  return (
    <div
      className="bg-white rounded-lg shadow-sm border p-4 hover:shadow-md transition-all cursor-pointer"
      onClick={onClick}
    >
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-rich font-semibold flex items-center gap-2">
            <span>#{lead.id}</span>
            {lead.name || 'Not Set'}
          </h3>
          {lead.segment && (
            <div className="text-xs px-2 py-0.5 bg-purple-50 text-purple-600 rounded-full mt-1 inline-block">
              {lead.segment}
            </div>
          )}
        </div>
        <div className="text-right">
          <div className="flex items-center gap-1 text-emerald-600 font-semibold">
            <IndianRupee size={16} />
            <span>{lead.budget ? `${lead.budget}L` : 'Not Set'}</span>
          </div>
          {lead.stage && (
            <div className="text-xs px-2 py-1 bg-blue-50 text-blue-600 rounded-full mt-1">
              {lead.stage}
            </div>
          )}
        </div>
      </div>

      <div className="space-y-3">
        {lead.next_action && lead.next_action_time && (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-600">
              <Target size={16} className="text-blue-500" />
              <span className="text-sm">{lead.next_action}</span>
            </div>
            <div className={`flex items-center gap-1 ${getTimeColor(lead.next_action_time)}`}>
              <Clock size={14} />
              <span className="text-xs">{formatDateTime(lead.next_action_time)}</span>
            </div>
          </div>
        )}

        {(lead.priority || lead.intent) && (
          <div className="flex items-center justify-between">
            {lead.priority && (
              <div className={`flex items-center gap-1 ${getPriorityColor(lead.priority)}`}>
                <BarChart3 size={16} />
                <span className="text-sm">{getPriorityLabel(lead.priority)}</span>
              </div>
            )}
            {lead.intent && (
              <div className="flex items-center gap-1 text-purple-600">
                <CircleDot size={16} />
                <span className="text-sm">Intent: {lead.intent}/10</span>
              </div>
            )}
          </div>
        )}

        {lead.tags && (
          <div className="flex items-center gap-2">
            <Tag size={16} className="text-orange-500 shrink-0" />
            <div className="flex flex-wrap gap-1">
              {lead.tags.split(',').map((tag, index) => (
                <span
                  key={index}
                  className="text-xs px-2 py-0.5 bg-orange-50 text-orange-600 rounded-full"
                >
                  {tag.trim()}
                </span>
              ))}
            </div>
          </div>
        )}

        {lead.requirement_description && (
          <div className="flex items-start gap-2 text-gray-600 mt-2">
            <MessageSquare size={16} className="text-gray-400 shrink-0 mt-1" />
            <p className="text-sm line-clamp-2">{lead.requirement_description}</p>
          </div>
        )}
      </div>
    </div>
  );
};