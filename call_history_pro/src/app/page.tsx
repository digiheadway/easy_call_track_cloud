
'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import dynamic from 'next/dynamic';
import { format, isToday, isYesterday, startOfDay, endOfDay, subDays } from 'date-fns';
import { getDateRangeFromPreset } from '@/app/components/filter-panel';
import Header from '@/app/components/header';
import { Loader2 } from 'lucide-react';
import {
  fetchCallers,
  updateCallerNote,
  updateCallNote,
  updateExcludedStatus,
  markSynced,
  updateCallerInfo,
  fetchSummary,
} from '@/lib/api';
import type { Caller, Call, GroupedCalls, CallGroup, SummaryData } from '@/lib/types';
import type { DateRange } from 'react-day-picker';
import { useToast } from '@/hooks/use-toast';
import { usePersistentState } from '@/hooks/use-persistent-state';

const CallLog = dynamic(() => import('@/app/components/call-log'), {
  loading: () => <div className="flex flex-1 items-center justify-center"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div>,
  ssr: false
});


export type LeadFilter = 'all' | 'lead' | 'not-lead';
export type CustomNameFilter = 'all' | 'set' | 'not-set';
export type TypeFilter = 'all' | 'set' | 'not-set';
export type NoteFilter = 'all' | 'with-note' | 'without-note';
export type SyncFilter = 'all' | 'done' | 'undone';
export type DateRangePreset = 'today' | 'yesterday' | 'last-3-days' | 'last-7-days' | 'last-14-days' | 'last-30-days' | 'this-month' | 'last-month' | 'last-3-months' | 'this-year' | 'all-time' | 'custom';

export default function Home() {
  const [loading, setLoading] = useState(true);
  const [allCallers, setAllCallers] = useState<Caller[]>([]);
  const [callsByPhone, setCallsByPhone] = useState<Record<string, Call[]>>({});
  const [summaryData, setSummaryData] = useState<SummaryData | null>(null);
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);

  const [searchQuery, setSearchQuery] = usePersistentState('searchQuery', '');
  const [activeTab, setActiveTab] = usePersistentState('activeTab', 'all');
  const [expandedAccordions, setExpandedAccordions] = usePersistentState<string[]>('expandedAccordions', []);

  const [datePreset, setDatePreset] = usePersistentState<DateRangePreset>('datePreset', 'last-3-days');
  const [useDateFilter, setUseDateFilter] = usePersistentState<boolean>('useDateFilter', false);
  const [dateRange, setDateRange] = usePersistentState<DateRange | undefined>('dateRange',
    getDateRangeFromPreset('last-3-days'),
    (value) => value && value.from && value.to ? { from: new Date(value.from), to: new Date(value.to) } : getDateRangeFromPreset('last-3-days')
  );

  const [currentlyPlaying, setCurrentlyPlaying] = usePersistentState<string | null>('currentlyPlaying', null);

  const [leadFilter, setLeadFilter] = usePersistentState<LeadFilter>('leadFilter', 'all');
  const [customNameFilter, setCustomNameFilter] = usePersistentState<CustomNameFilter>('customNameFilter', 'all');
  const [typeFilter, setTypeFilter] = usePersistentState<TypeFilter>('typeFilter', 'all');
  const [noteFilter, setNoteFilter] = usePersistentState<NoteFilter>('noteFilter', 'all');
  const [syncFilter, setSyncFilter] = usePersistentState<SyncFilter>('syncFilter', 'all');


  const { toast } = useToast();

  const toggleAccordion = (id: string) => {
    setExpandedAccordions(prev => {
      const isAlreadyExpanded = prev.includes(id);
      if (isAlreadyExpanded) {
        return prev.filter(item => item !== id);
      } else {
        const newExpanded = [...prev, id];
        // Keep max 2 accordions open
        if (newExpanded.length > 2) {
          return newExpanded.slice(newExpanded.length - 2);
        }
        return newExpanded;
      }
    });
  };

  const fetchAndSetCallers = useCallback(async () => {
    setLoading(true);
    try {
      const startDate = (useDateFilter && dateRange?.from) ? format(startOfDay(dateRange.from), 'yyyy-MM-dd') : undefined;
      const endDate = (useDateFilter && dateRange?.to) ? format(endOfDay(dateRange.to), 'yyyy-MM-dd') : startDate;

      const fetchedCallers = await fetchCallers({
        start_date: startDate,
        end_date: endDate,
        search_query: searchQuery,
      });
      setAllCallers(fetchedCallers);

    } catch (error) {
      console.error('Failed to fetch callers:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not fetch call data from the server.',
      });
    } finally {
      setLoading(false);
    }
  }, [dateRange, searchQuery, useDateFilter, toast]);

  const fetchAndSetSummary = useCallback(async (range: DateRange | undefined, useFilter: boolean) => {
    if (!useFilter || !range || !range.from) {
      setSummaryData(null);
      return;
    }

    setIsSummaryLoading(true);
    try {
      const startDate = format(startOfDay(range.from), 'yyyy-MM-dd');
      const endDate = format(endOfDay(range.to || range.from), 'yyyy-MM-dd');
      const summary = await fetchSummary({ start_date: startDate, end_date: endDate });
      setSummaryData(summary);
    } catch (error) {
      console.error('Failed to fetch summary:', error);
      // Optional: show a toast, but maybe it's better to fail silently
    } finally {
      setIsSummaryLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAndSetCallers();
  }, [fetchAndSetCallers]);

  useEffect(() => {
    fetchAndSetSummary(dateRange, useDateFilter);
  }, [dateRange, useDateFilter, fetchAndSetSummary]);

  const handleUpdateContactNote = async (callerId: string, newNote: string) => {
    try {
      await updateCallerNote(callerId, newNote);
      setAllCallers((prevCallers) =>
        prevCallers.map((caller) =>
          caller.id === callerId ? { ...caller, note: newNote, last_sync: false } : caller
        )
      );
      toast({
        title: 'Note Saved',
        description: 'The contact note has been updated.',
      });
    } catch (error) {
      console.error('Failed to update caller note:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not save the contact note.',
      });
    }
  };

  const handleUpdateCallNote = async (callId: string, newNote: string) => {
    try {
      await updateCallNote(callId, newNote);
      setCallsByPhone(prev => {
        const newCallsByPhone = { ...prev };
        for (const phone in newCallsByPhone) {
          newCallsByPhone[phone] = newCallsByPhone[phone].map(call =>
            call.id === callId ? { ...call, note: newNote } : call
          );
        }
        return newCallsByPhone;
      });
      toast({
        title: 'Call Note Saved',
        description: 'The note for this specific call has been updated.',
      });
    } catch (error) {
      console.error('Failed to update call note:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not save the call note.',
      });
    }
  };

  const handleExcludeNumber = async (callerId: string) => {
    try {
      await updateExcludedStatus(callerId, true);
      setAllCallers((prev) => prev.filter((c) => c.id !== callerId));
      toast({
        title: 'Contact Excluded',
        description: `Contact has been removed from the list.`,
      });
    } catch (error) {
      console.error('Failed to exclude number:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not exclude the contact.',
      });
    }
  };

  const handleMarkSynced = async (callerId: string, currentStatus: boolean) => {
    try {
      await markSynced(callerId, currentStatus);
      setAllCallers((prev) =>
        prev.map((caller) =>
          caller.id === callerId ? { ...caller, last_sync: !currentStatus } : caller
        )
      );
      toast({
        title: 'Contact Status Updated',
        description: `Contact has been marked as ${!currentStatus ? 'done' : 'not done'}.`,
      });
    } catch (error) {
      console.error('Failed to mark as synced:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not update the contact sync status.',
      });
    }
  };

  const handleUpdateCallerInfo = async (callerId: string, info: { custom_name?: string; caller_type?: string }) => {
    try {
      await updateCallerInfo(callerId, info);
      setAllCallers((prevCallers) =>
        prevCallers.map((caller) =>
          caller.id === callerId ? { ...caller, ...info, last_sync: false } : caller
        )
      );
      toast({
        title: 'Info Updated',
        description: 'The contact information has been saved.',
      });
    } catch (error) {
      console.error('Failed to update caller info:', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Could not save the contact information.',
      });
    }
  };

  const filteredCallers = useMemo(() => {
    return allCallers.filter(caller => {
      if (leadFilter === 'lead' && !caller.lead_id) return false;
      if (leadFilter === 'not-lead' && caller.lead_id) return false;

      if (customNameFilter === 'set' && !caller.custom_name) return false;
      if (customNameFilter === 'not-set' && caller.custom_name) return false;

      if (typeFilter === 'set' && !caller.caller_type) return false;
      if (typeFilter === 'not-set' && caller.caller_type) return false;

      if (noteFilter === 'with-note' && !caller.note) return false;
      if (noteFilter === 'without-note' && caller.note) return false;

      if (syncFilter === 'done' && !caller.last_sync) return false;
      if (syncFilter === 'undone' && caller.last_sync) return false;

      return true;
    });
  }, [allCallers, leadFilter, customNameFilter, typeFilter, noteFilter, syncFilter]);


  const getGroupTitle = useCallback((date: Date) => {
    const startOfDate = startOfDay(date);
    if (isToday(startOfDate)) return 'Today';
    if (isYesterday(startOfDate)) return 'Yesterday';
    return format(startOfDate, 'MMMM d, yyyy');
  }, []);

  const { groupedAndSortedCalls, sortedGroupTitles } = useMemo(() => {
    const callGroups = filteredCallers
      .sort((a, b) => new Date(b.last_call).getTime() - new Date(a.last_call).getTime())
      .map((caller): CallGroup => ({
        caller: caller,
        calls: callsByPhone[caller.phone] || [],
        lastCallTimestamp: new Date(caller.last_call),
      }));

    const groupsByTitle = callGroups.reduce((acc, group) => {
      const title = getGroupTitle(group.lastCallTimestamp);
      if (!acc[title]) {
        acc[title] = [];
      }
      acc[title].push(group);
      return acc;
    }, {} as Record<string, CallGroup[]>);

    const finalGroupedCalls: GroupedCalls = {};
    for (const title in groupsByTitle) {
      const groups = groupsByTitle[title];
      const callCount = groups.reduce((sum, g) => sum + g.caller.calls_in_range, 0);
      const callerCount = groups.length;
      finalGroupedCalls[title] = {
        groups: groups,
        callCount,
        callerCount
      };
    }

    const sortedTitles = Object.keys(finalGroupedCalls).sort((a, b) => {
      let aDate: Date;
      let bDate: Date;

      if (a === 'Today') aDate = new Date();
      else if (a === 'Yesterday') aDate = subDays(new Date(), 1);
      else aDate = new Date(a);

      if (b === 'Today') bDate = new Date();
      else if (b === 'Yesterday') bDate = subDays(new Date(), 1);
      else bDate = new Date(b);

      return bDate.getTime() - aDate.getTime();
    });

    return { groupedAndSortedCalls: finalGroupedCalls, sortedGroupTitles: sortedTitles };

  }, [filteredCallers, callsByPhone, getGroupTitle]);

  return (
    <div className="flex h-full flex-col">
      <Header
        onDateRangeChange={setDateRange}
        onDatePresetChange={setDatePreset}
        initialRange={dateRange}
        datePreset={datePreset}
        useDateFilter={useDateFilter}
        setUseDateFilter={setUseDateFilter}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        leadFilter={leadFilter}
        setLeadFilter={setLeadFilter}
        customNameFilter={customNameFilter}
        setCustomNameFilter={setCustomNameFilter}
        typeFilter={typeFilter}
        setTypeFilter={setTypeFilter}
        noteFilter={noteFilter}
        setNoteFilter={setNoteFilter}
        syncFilter={syncFilter}
        setSyncFilter={setSyncFilter}
      />
      {loading ? (
        <div className="flex flex-1 items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : (
        <CallLog
          groupedCalls={groupedAndSortedCalls}
          sortedGroupTitles={sortedGroupTitles}
          onUpdateContactNote={handleUpdateContactNote}
          onUpdateCallNote={handleUpdateCallNote}
          onExcludeNumber={handleExcludeNumber}
          onMarkSynced={handleMarkSynced}
          onUpdateCallerInfo={handleUpdateCallerInfo}
          allCallers={allCallers}
          setCallsByPhone={setCallsByPhone}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          expandedAccordions={expandedAccordions}
          toggleAccordion={toggleAccordion}
          currentlyPlaying={currentlyPlaying}
          setCurrentlyPlaying={setCurrentlyPlaying}
          summaryData={summaryData}
          isSummaryLoading={isSummaryLoading}
        />
      )}
    </div>
  );
}
