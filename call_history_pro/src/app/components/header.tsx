
'use client';
import { Phone, SlidersHorizontal } from 'lucide-react';
import { Button } from "@/components/ui/button";
import { useState } from "react";
import type { LeadFilter, CustomNameFilter, TypeFilter, NoteFilter, SyncFilter, DateRangePreset } from '@/app/page';
import type { DateRange } from 'react-day-picker';
import dynamic from 'next/dynamic';

const FilterPanel = dynamic(() => import('./filter-panel'));

interface HeaderProps {
  onDateRangeChange: (range: DateRange | undefined) => void;
  onDatePresetChange: (preset: DateRangePreset) => void;
  initialRange?: DateRange;
  datePreset: DateRangePreset;
  useDateFilter: boolean;
  setUseDateFilter: (use: boolean) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  leadFilter: LeadFilter;
  setLeadFilter: (filter: LeadFilter) => void;
  customNameFilter: CustomNameFilter;
  setCustomNameFilter: (filter: CustomNameFilter) => void;
  typeFilter: TypeFilter;
  setTypeFilter: (filter: TypeFilter) => void;
  noteFilter: NoteFilter;
  setNoteFilter: (filter: NoteFilter) => void;
  syncFilter: SyncFilter;
  setSyncFilter: (filter: SyncFilter) => void;
}

export default function Header({
  onDateRangeChange,
  onDatePresetChange,
  initialRange,
  datePreset,
  useDateFilter,
  setUseDateFilter,
  searchQuery,
  setSearchQuery,
  leadFilter,
  setLeadFilter,
  customNameFilter,
  setCustomNameFilter,
  typeFilter,
  setTypeFilter,
  noteFilter,
  setNoteFilter,
  syncFilter,
  setSyncFilter,
}: HeaderProps) {
  const [isPanelOpen, setIsPanelOpen] = useState(false);

  return (
    <>
      <header className="flex shrink-0 items-center justify-between border-b bg-card/80 px-4 py-3 backdrop-blur-sm">
        <div className="flex items-center gap-4">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
            <Phone className="h-5 w-5 text-primary-foreground" />
          </div>
          <h1 className="text-xl font-bold text-foreground">CallSync</h1>
        </div>
        <Button variant="ghost" size="icon" onClick={() => setIsPanelOpen(true)}>
          <SlidersHorizontal className="h-5 w-5" />
          <span className="sr-only">Open filters</span>
        </Button>
      </header>
      {isPanelOpen && (
        <FilterPanel
          isOpen={isPanelOpen}
          onClose={() => setIsPanelOpen(false)}
          onDateRangeChange={onDateRangeChange}
          onDatePresetChange={onDatePresetChange}
          initialRange={initialRange}
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
      )}
    </>
  );
}
