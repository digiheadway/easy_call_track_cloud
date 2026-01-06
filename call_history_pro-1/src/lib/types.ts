

export interface Caller {
  id: string;
  phone: string;
  custom_name: string | null;
  caller_type: string | null;
  note: string | null;
  calls: number;
  calls_in_range: number;
  last_call: string;
  last_call_type: 'incoming' | 'outgoing' | 'missed' | 'rejected';
  last_call_duration: number;
  excluded: boolean;
  last_sync: boolean;
  lead_id: string | null;
  lead_name: string | null;
  segment: string | null;
  budget: string | null;
  address: string | null;
  about_him: string | null;
  lead_note: string | null;
}

export interface Call {
  id: string;
  phone: string;
  note: string | null;
  duration: number;
  type: 'incoming' | 'outgoing' | 'missed' | 'rejected';
  recording_url: string | null;
  created_ts: string;
}

export type DateRange = {
  from?: Date;
  to?: Date;
};

export interface CallGroup {
  caller: Caller;
  calls: Call[];
  lastCallTimestamp: Date;
}

export type GroupedCalls = Record<string, {
  groups: CallGroup[];
  callCount: number;
  callerCount: number;
}>;

export interface DateSummary {
  [date: string]: number;
}

export interface SummaryData {
  callers: DateSummary;
  calls: DateSummary;
}


export interface Lead {
  id: string;
  name: string;
  phone: string;
  alternative_contact_details: string | null;
  address: string;
  about_him: string;
  requirement_description: string;
  note: string;
  budget: string;
  preferred_area: string;
  size: string;
  preferred_type: string;
  purpose: string;
  stage: string;
  deal_status: string | null;
  visit_status: string | null;
  when_buy: string | null;
  priority: string;
  next_action: string | null;
  next_action_time: string | null;
  next_action_note: string | null;
  interested_in: string | null;
  intent: string;
  not_interested_in: string | null;
  assigned_to: string;
  source: string;
  medium: string | null;
  placement: string | null;
  list_name: string | null;
  tags: string;
  data_1: string | null;
  data_2: string | null;
  data_3: string;
  segment: string;
  created_at: string;
  updated_at: string;
  is_deleted: string;
  tasks: any[];
}
