export type LeadStage =
  | 'Fresh Lead'
  | 'Contacted'
  | 'Qualified'
  | 'Proposal'
  | 'Negotiation'
  | 'Closed Won'
  | 'Closed Lost';

export type TodoType =
  | 'Follow Up'
  | 'Meeting'
  | 'Find Match'
  | 'Schedule Site Visit'
  | 'Todo'
  | 'Activity'
  | 'Other';

export type TodoStatus = 'Pending' | 'Completed' | 'Cancelled' | 'Overdue';

export type PurchaseTimeline =
  | 'ASAP'
  | '1 Month'
  | '3 Months'
  | '6 Months'
  | '1 Year';

export type Priority = 'Low' | 'Medium' | 'High' | 'Urgent';

export type Source =
  | 'Referral'
  | 'Website'
  | 'Social Media'
  | 'Cold Call'
  | 'Facebook'
  | 'Instagram'
  | 'Other';

export type Medium = 'Post' | 'Story' | 'Reel' | 'Direct' | 'Other';

export type Placement = 'Main Page' | 'Story' | 'Search' | 'Other';

export type NextAction =
  | 'Call Back'
  | 'Site Visit'
  | 'Follow Up'
  | 'Meeting'
  | 'Other';

export type Segment = 
  | 'Panipat'
  | 'Panipat Projects'
  | 'Panipat Investors'
  | 'Rohtak'
  | 'Sonipat'
  | 'Sigma'
  | 'Trident'
  | 'Godrej'
  | 'M3M';

export type Purpose = 'Self Use' | 'Investment' | 'Rental' | 'Resale' | 'Other';

export interface Lead {
  id: number;
  name: string;
  phone: string;
  alternatePhone?: string;
  address?: string;
  stage: LeadStage;
  ourRating?: string;
  budget: number;
  preferredLocation: string[];
  preferredSize: string[];
  note?: string;
  requirementDescription?: string;
  propertyType: string[];
  purchaseTimeline: PurchaseTimeline;
  purpose: Purpose;
  about?: string;
  segment: Segment;
  source: Source;
  medium?: Medium;
  placement?: Placement;
  priority: Priority;
  nextAction?: NextAction;
  nextActionNote?: string;
  interestedIn?: string;
  notInterestedIn?: string;
  listName?: string;
  tags: string[];
  assignedTo: string[];
  data1?: string;
  data2?: string;
  data3?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted?: boolean;
}

export interface Todo {
  id: number;
  leadId: number;
  type: TodoType;
  description?: string;
  responseNote?: string;
  status: TodoStatus;
  dateTime: string;
  participants: string[];
  createdAt: string;
  updatedAt: string;
}

export interface TagOption {
  value: string;
  label: string;
}

export interface DropdownOption {
  value: string;
  label: string;
  searchable?: boolean;
}

export interface FilterOption {
  field: string;
  operator: string;
  value: string | number | string[];
}
