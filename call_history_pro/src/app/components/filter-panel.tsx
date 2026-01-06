
'use client';
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { useState, useEffect } from "react";
import { DateRange } from "react-day-picker";
import { format, subDays, startOfDay, endOfDay, startOfMonth, endOfMonth, startOfYear, subMonths } from 'date-fns';
import { Calendar as CalendarIcon, Search } from 'lucide-react';
import type { LeadFilter, CustomNameFilter, TypeFilter, NoteFilter, SyncFilter, DateRangePreset } from '@/app/page';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

interface FilterPanelProps {
  isOpen: boolean;
  onClose: () => void;
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

export function getDateRangeFromPreset(preset: DateRangePreset): DateRange | undefined {
  const today = startOfDay(new Date());

  switch (preset) {
    case 'today':
      return { from: today, to: endOfDay(today) };
    case 'yesterday':
      const yesterday = subDays(today, 1);
      return { from: yesterday, to: endOfDay(yesterday) };
    case 'last-3-days':
      return { from: subDays(today, 2), to: endOfDay(today) };
    case 'last-7-days':
      return { from: subDays(today, 6), to: endOfDay(today) };
    case 'last-14-days':
      return { from: subDays(today, 13), to: endOfDay(today) };
    case 'last-30-days':
      return { from: subDays(today, 29), to: endOfDay(today) };
    case 'this-month':
      return { from: startOfMonth(today), to: endOfDay(today) };
    case 'last-month':
      const lastMonth = subMonths(today, 1);
      return { from: startOfMonth(lastMonth), to: endOfMonth(lastMonth) };
    case 'last-3-months':
      return { from: startOfMonth(subMonths(today, 2)), to: endOfDay(today) };
    case 'this-year':
      return { from: startOfYear(today), to: endOfDay(today) };
    case 'all-time':
      return undefined;
    case 'custom':
      return undefined; // Will be set by calendar
    default:
      return undefined;
  }
}

export default function FilterPanel({
  isOpen,
  onClose,
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
  setSyncFilter
}: FilterPanelProps) {

  const [isDatePopoverOpen, setIsDatePopoverOpen] = useState(false);

  const handlePresetChange = (value: DateRangePreset) => {
    onDatePresetChange(value);

    if (value === 'custom') {
      setIsDatePopoverOpen(true);
      return;
    }

    const newRange = getDateRangeFromPreset(value);
    onDateRangeChange(newRange);
    setIsDatePopoverOpen(false);
  }

  const handleDateSelect = (range: DateRange | undefined) => {
    if (range?.from) {
      onDateRangeChange({ from: startOfDay(range.from), to: range.to ? endOfDay(range.to) : endOfDay(range.from) });
    } else {
      onDateRangeChange(undefined);
    }

    if (range?.from && range.to) {
      setIsDatePopoverOpen(false);
    }
  }

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent onOpenAutoFocus={(e) => e.preventDefault()} className="flex flex-col">
        <SheetHeader>
          <SheetTitle>Filters & Search</SheetTitle>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto">
          <div className="space-y-6 p-4">
            <div>
              <h3 className="mb-2 text-sm font-medium text-foreground">Search</h3>
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="Search by name or number..."
                  className="pl-9"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <div className="flex items-center space-x-2 mt-3">
                <Checkbox
                  id="use-date-filter"
                  checked={useDateFilter}
                  onCheckedChange={(checked) => setUseDateFilter(checked === true)}
                />
                <label
                  htmlFor="use-date-filter"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  Search with date filter
                </label>
              </div>
            </div>

            <Separator />

            <div>
              <h3 className="mb-2 text-sm font-medium text-foreground">Date Range</h3>
              <div className="flex flex-col gap-2">
                <Select value={datePreset} onValueChange={handlePresetChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select range preset" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="today">Today</SelectItem>
                    <SelectItem value="yesterday">Yesterday</SelectItem>
                    <SelectItem value="last-3-days">Last 3 Days</SelectItem>
                    <SelectItem value="last-7-days">Last 7 Days</SelectItem>
                    <SelectItem value="last-14-days">Last 14 Days</SelectItem>
                    <SelectItem value="last-30-days">Last 30 Days</SelectItem>
                    <SelectItem value="this-month">This Month</SelectItem>
                    <SelectItem value="last-month">Last Month</SelectItem>
                    <SelectItem value="last-3-months">Last Three Months</SelectItem>
                    <SelectItem value="this-year">This Year</SelectItem>
                    <SelectItem value="all-time">All Time</SelectItem>
                    <SelectItem value="custom">Custom</SelectItem>
                  </SelectContent>
                </Select>

                {datePreset === 'custom' && (
                  <Popover open={isDatePopoverOpen} onOpenChange={setIsDatePopoverOpen}>
                    <PopoverTrigger asChild>
                      <Button
                        id="date"
                        variant={"outline"}
                        className={cn(
                          "w-full justify-start text-left font-normal",
                          !initialRange && "text-muted-foreground"
                        )}
                      >
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {initialRange?.from ? (
                          initialRange.to ? (
                            <>
                              {format(initialRange.from, "LLL dd, y")} -{" "}
                              {format(initialRange.to, "LLL dd, y")}
                            </>
                          ) : (
                            format(initialRange.from, "LLL dd, y")
                          )
                        ) : (
                          <span>Pick a date</span>
                        )}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        initialFocus
                        mode="range"
                        defaultMonth={initialRange?.from}
                        selected={initialRange}
                        onSelect={handleDateSelect}
                        numberOfMonths={1}
                      />
                    </PopoverContent>
                  </Popover>
                )}

              </div>
            </div>

            <Separator />

            <div className="space-y-4">
              <div>
                <Label className="text-sm font-medium text-foreground">Lead Status</Label>
                <Select value={leadFilter} onValueChange={value => setLeadFilter(value as LeadFilter)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by lead status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="lead">Leads Only</SelectItem>
                    <SelectItem value="not-lead">Not Leads</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm font-medium text-foreground">Custom Name</Label>
                <Select value={customNameFilter} onValueChange={value => setCustomNameFilter(value as CustomNameFilter)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by custom name" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="set">Name Set</SelectItem>
                    <SelectItem value="not-set">Name Not Set</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm font-medium text-foreground">Caller Type</Label>
                <Select value={typeFilter} onValueChange={value => setTypeFilter(value as TypeFilter)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by caller type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="set">Type Set</SelectItem>
                    <SelectItem value="not-set">Type Not Set</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm font-medium text-foreground">Contact Note</Label>
                <Select value={noteFilter} onValueChange={value => setNoteFilter(value as NoteFilter)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by note" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="with-note">With Note</SelectItem>
                    <SelectItem value="without-note">Without Note</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm font-medium text-foreground">Sync Status</Label>
                <Select value={syncFilter} onValueChange={value => setSyncFilter(value as SyncFilter)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by sync status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="done">Done</SelectItem>
                    <SelectItem value="undone">Not Done</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
        </div>
        <div className="border-t p-4">
          <Button onClick={onClose} className="w-full">Done</Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
