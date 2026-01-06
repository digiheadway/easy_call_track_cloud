
'use client';

import type { Caller, Call, GroupedCalls, SummaryData } from '@/lib/types';
import { ScrollArea } from '@/components/ui/scroll-area';
import { CallGroupCard } from '@/app/components/call-card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useMemo } from 'react';
import { Separator } from '@/components/ui/separator';
import { usePersistentScroll } from '@/hooks/use-persistent-scroll';
import { format, isToday, isYesterday, parse } from 'date-fns';

export interface CallGroup {
  caller: Caller;
  calls: Call[];
  lastCallTimestamp: Date;
}

interface CallLogProps {
  groupedCalls: GroupedCalls;
  sortedGroupTitles: string[];
  onUpdateContactNote: (callerId: string, newNote: string) => void;
  onUpdateCallNote: (callId: string, newNote: string) => void;
  onUpdateCallerInfo: (callerId: string, info: { custom_name?: string; caller_type?: string }) => void;
  onExcludeNumber: (callerId: string) => void;
  onMarkSynced: (callerId: string, currentStatus: boolean) => void;
  allCallers: Caller[];
  setCallsByPhone: React.Dispatch<React.SetStateAction<Record<string, Call[]>>>;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  expandedAccordions: string[];
  toggleAccordion: (id: string) => void;
  currentlyPlaying: string | null;
  setCurrentlyPlaying: (id: string | null) => void;
  summaryData: SummaryData | null;
  isSummaryLoading: boolean;
}

const CallGroupList = ({
  groups,
  sortedGroupTitles,
  onUpdateContactNote,
  onUpdateCallNote,
  onUpdateCallerInfo,
  onExcludeNumber,
  onMarkSynced,
  setCallsByPhone,
  tab,
  scrollAreaRef,
  expandedAccordions,
  toggleAccordion,
  currentlyPlaying,
  setCurrentlyPlaying,
  summaryData,
  isSummaryLoading,
}: {
  groups: GroupedCalls;
  sortedGroupTitles: string[];
  onUpdateContactNote: (callerId: string, newNote: string) => void;
  onUpdateCallNote: (callId: string, newNote: string) => void;
  onUpdateCallerInfo: (callerId: string, info: { custom_name?: string; caller_type?: string }) => void;
  onExcludeNumber: (callerId: string) => void;
  onMarkSynced: (callerId: string, currentStatus: boolean) => void;
  setCallsByPhone: React.Dispatch<React.SetStateAction<Record<string, Call[]>>>;
  tab: string;
  scrollAreaRef: React.RefObject<HTMLDivElement>;
  expandedAccordions: string[];
  toggleAccordion: (id: string) => void;
  currentlyPlaying: string | null;
  setCurrentlyPlaying: (id: string | null) => void;
  summaryData: SummaryData | null;
  isSummaryLoading: boolean;
}) => {
  const hasCalls = sortedGroupTitles.some(title => groups[title] && groups[title].groups.length > 0);

  const getSummaryForTitle = (title: string): string => {
    if (isSummaryLoading) return '(.../...)';
    
    let dateKey: string;
    if (title === 'Today') {
        dateKey = format(new Date(), 'yyyy-MM-dd');
    } else if (title === 'Yesterday') {
        dateKey = format(new Date(new Date().setDate(new Date().getDate() - 1)), 'yyyy-MM-dd');
    } else {
        try {
            dateKey = format(parse(title, 'MMMM d, yyyy', new Date()), 'yyyy-MM-dd');
        } catch {
             // Fallback for titles that aren't dates
            return `(${groups[title].callCount}/${groups[title].callerCount})`;
        }
    }
    
    if (summaryData) {
        const calls = summaryData.calls[dateKey] ?? 0;
        const callers = summaryData.callers[dateKey] ?? 0;
        return `(${calls}/${callers})`;
    }
    
    return `(${groups[title].callCount}/${groups[title].callerCount})`;
  };

  return (
    <ScrollArea className="h-full" viewportRef={scrollAreaRef}>
      <div className="space-y-4 p-4 pt-2">
        {hasCalls ? (
          sortedGroupTitles.map(title => {
            const groupInfo = groups[title];
            if (!groupInfo || groupInfo.groups.length === 0) return null;
            
            const titleText = title.startsWith('Today') || title.startsWith('Yesterday') 
              ? title.split(' (')[0]
              : title;
            
            const stats = getSummaryForTitle(title);

            return (
              <div key={title}>
                <div className="flex items-center gap-4">
                  <Separator className="flex-1" />
                  <h3 className="whitespace-nowrap text-xs font-medium text-muted-foreground">{titleText} <span className="font-normal">{stats}</span></h3>
                  <Separator className="flex-1" />
                </div>
                <div className="mt-2 space-y-2">
                  {groupInfo.groups.map(group => (
                    <CallGroupCard
                      key={`${tab}-${group.caller.id}`}
                      group={group}
                      onUpdateContactNote={onUpdateContactNote}
                      onUpdateCallNote={onUpdateCallNote}
                      onUpdateCallerInfo={onUpdateCallerInfo}
                      onExcludeNumber={onExcludeNumber}
                      onMarkSynced={onMarkSynced}
                      setCallsByPhone={setCallsByPhone}
                      isExpanded={expandedAccordions.includes(group.caller.id)}
                      onToggleExpand={() => toggleAccordion(group.caller.id)}
                      currentlyPlaying={currentlyPlaying}
                      setCurrentlyPlaying={setCurrentlyPlaying}
                    />
                  ))}
                </div>
              </div>
            )
          })
        ) : <NoCallsMessage tab={tab} />}
      </div>
    </ScrollArea>
  );
};


const NoCallsMessage = ({ tab }: { tab: string }) => (
    <div className="flex h-64 items-center justify-center text-muted-foreground">
      <p>No {tab.toLowerCase()} calls in the selected date range</p>
    </div>
);

export default function CallLog({ 
    groupedCalls, 
    sortedGroupTitles, 
    onUpdateContactNote, 
    onUpdateCallNote, 
    onUpdateCallerInfo,
    onExcludeNumber, 
    onMarkSynced,
    allCallers,
    setCallsByPhone,
    activeTab,
    setActiveTab,
    expandedAccordions,
    toggleAccordion,
    currentlyPlaying,
    setCurrentlyPlaying,
    summaryData,
    isSummaryLoading,
}: CallLogProps) {
  
  const scrollRef = usePersistentScroll('callLogScroll');

  const allGroups = useMemo(() => sortedGroupTitles.flatMap(title => groupedCalls[title]?.groups || []), [groupedCalls, sortedGroupTitles]);
  
  const connectedGroups = useMemo(() => allGroups.filter(g => g.caller.last_call_duration >= 5), [allGroups]);
  const connectedIds = useMemo(() => new Set(connectedGroups.map(g => g.caller.id)), [connectedGroups]);

  const missedGroups = useMemo(() => allGroups.filter(g => 
    (g.caller.last_call_type === 'missed' && g.caller.last_call_duration === 0) ||
    (g.caller.last_call_type === 'incoming' && g.caller.last_call_duration > 0 && g.caller.last_call_duration < 5)
  ), [allGroups]);
  const missedIds = useMemo(() => new Set(missedGroups.map(g => g.caller.id)), [missedGroups]);

  const rejectedGroups = useMemo(() => allGroups.filter(g => g.caller.last_call_type === 'rejected'), [allGroups]);
  
  const outgoingFailedGroups = useMemo(() => {
    return allGroups.filter(g => {
      const isOutgoingFailed = g.caller.last_call_type === 'outgoing' && g.caller.last_call_duration < 5;
      return isOutgoingFailed && !connectedIds.has(g.caller.id);
    });
  }, [allGroups, connectedIds]);
  const outgoingFailedIds = useMemo(() => new Set(outgoingFailedGroups.map(g => g.caller.id)), [outgoingFailedGroups]);

  const phoneNumbersWithConnection = useMemo(() => {
    return new Set(allCallers.filter(c => c.last_call_duration >= 5).map(c => c.phone));
  }, [allCallers]);
  
  const neverAttendedGroups = useMemo(() => {
    return allGroups.filter(g => {
      if (g.caller.calls < 2) return false;
      if (phoneNumbersWithConnection.has(g.caller.phone)) return false;
      return outgoingFailedIds.has(g.caller.id);
    });
  }, [allGroups, phoneNumbersWithConnection, outgoingFailedIds]);
  
  const filterGroupsByTitle = (groups: CallGroup[]) => {
    const titleMap: Record<string, CallGroup[]> = {};
    const groupIds = new Set(groups.map(g => g.caller.id));

    for (const title of sortedGroupTitles) {
        const titleGroups = groupedCalls[title]?.groups || [];
        const matchingGroups = titleGroups.filter(g => groupIds.has(g.caller.id));
        if (matchingGroups.length > 0) {
            titleMap[title] = matchingGroups;
        }
    }

    const result: GroupedCalls = {};
    for(const title in titleMap) {
        const subGroups = titleMap[title];
        result[title] = {
            groups: subGroups,
            callCount: subGroups.reduce((acc, g) => acc + g.caller.calls_in_range, 0),
            callerCount: subGroups.length
        }
    }
    return result;
  };

  const getSortedTitlesForGroups = (groups: CallGroup[]) => {
      const titles = new Set<string>();
      const groupIds = new Set(groups.map(g => g.caller.id));

      for (const title of sortedGroupTitles) {
          if (groupedCalls[title]?.groups.some(g => groupIds.has(g.caller.id))) {
              titles.add(title);
          }
      }
      return sortedGroupTitles.filter(t => titles.has(t));
  };
  
  const groupedConnected = useMemo(() => filterGroupsByTitle(connectedGroups), [connectedGroups, sortedGroupTitles, groupedCalls]);
  const sortedConnectedTitles = useMemo(() => getSortedTitlesForGroups(connectedGroups), [connectedGroups, sortedGroupTitles, groupedCalls]);

  const groupedMissed = useMemo(() => filterGroupsByTitle(missedGroups), [missedGroups, sortedGroupTitles, groupedCalls]);
  const sortedMissedTitles = useMemo(() => getSortedTitlesForGroups(missedGroups), [missedGroups, sortedGroupTitles, groupedCalls]);

  const groupedRejected = useMemo(() => filterGroupsByTitle(rejectedGroups), [rejectedGroups, sortedGroupTitles, groupedCalls]);
  const sortedRejectedTitles = useMemo(() => getSortedTitlesForGroups(rejectedGroups), [rejectedGroups, sortedGroupTitles, groupedCalls]);
    
  const groupedOutgoingFailed = useMemo(() => filterGroupsByTitle(outgoingFailedGroups), [outgoingFailedGroups, sortedGroupTitles, groupedCalls]);
  const sortedOutgoingFailedTitles = useMemo(() => getSortedTitlesForGroups(outgoingFailedGroups), [outgoingFailedGroups, sortedGroupTitles, groupedCalls]);
  
  const groupedNeverAttended = useMemo(() => filterGroupsByTitle(neverAttendedGroups), [neverAttendedGroups, sortedGroupTitles, groupedCalls]);
  const sortedNeverAttendedTitles = useMemo(() => getSortedTitlesForGroups(neverAttendedGroups), [neverAttendedGroups, sortedGroupTitles, groupedCalls]);

  const listProps = { onUpdateContactNote, onUpdateCallNote, onUpdateCallerInfo, onExcludeNumber, onMarkSynced, setCallsByPhone, scrollAreaRef: scrollRef, expandedAccordions, toggleAccordion, currentlyPlaying, setCurrentlyPlaying, summaryData, isSummaryLoading };

  return (
    <Tabs value={activeTab} onValueChange={setActiveTab} className="flex flex-1 flex-col overflow-hidden">
      <div className="overflow-x-auto px-4 pt-4">
        <div className="inline-block">
          <TabsList>
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="connected">Connected</TabsTrigger>
            <TabsTrigger value="missed">Missed</TabsTrigger>
            <TabsTrigger value="rejected">Rejected</TabsTrigger>
            <TabsTrigger value="outgoing-failed">Outgoing Failed</TabsTrigger>
            <TabsTrigger value="never-attended">Never Attended</TabsTrigger>
          </TabsList>
        </div>
      </div>
      <TabsContent value="all" className="flex-1 overflow-hidden">
        <CallGroupList 
            groups={groupedCalls} 
            sortedGroupTitles={sortedGroupTitles} 
            tab="All"
            {...listProps}
        />
      </TabsContent>
       <TabsContent value="connected" className="flex-1 overflow-hidden">
         <CallGroupList 
            groups={groupedConnected} 
            sortedGroupTitles={sortedConnectedTitles}
            tab="Connected"
            {...listProps}
        />
      </TabsContent>
      <TabsContent value="missed" className="flex-1 overflow-hidden">
         <CallGroupList 
            groups={groupedMissed} 
            sortedGroupTitles={sortedMissedTitles}
            tab="Missed"
            {...listProps}
        />
      </TabsContent>
      <TabsContent value="rejected" className="flex-1 overflow-hidden">
        <CallGroupList 
            groups={groupedRejected} 
            sortedGroupTitles={sortedRejectedTitles}
            tab="Rejected"
            {...listProps}
        />
      </TabsContent>
      <TabsContent value="outgoing-failed" className="flex-1 overflow-hidden">
        <CallGroupList 
            groups={groupedOutgoingFailed} 
            sortedGroupTitles={sortedOutgoingFailedTitles}
            tab="Outgoing Failed"
            {...listProps}
        />
      </TabsContent>
      <TabsContent value="never-attended" className="flex-1 overflow-hidden">
         <CallGroupList 
            groups={groupedNeverAttended}
            sortedGroupTitles={sortedNeverAttendedTitles}
            tab="Never Attended"
            {...listProps}
        />
      </TabsContent>
    </Tabs>
  );
}
