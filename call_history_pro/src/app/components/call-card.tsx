
'use client';

import React, { useState, useEffect } from 'react';
import type { Caller } from '@/lib/types';
import { fetchCalls } from '@/lib/api';
import type { Call, CallGroup } from './call-log';
import { formatDistanceToNow, format } from 'date-fns';
import {
  ArrowDownLeft,
  ArrowUpRight,
  PhoneMissed,
  XCircle,
  PhoneOff,
  MoreVertical,
  Clock,
  Loader2,
  Speaker,
  Phone,
  MessageCircle,
  Copy,
  Info,
  CheckCircle2,
  Edit,
  ExternalLink,
} from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { Badge } from '@/components/ui/badge';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator } from '@/components/ui/dropdown-menu';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { AudioPlayer } from './audio-player';
import dynamic from 'next/dynamic';

const LeadInfoSheet = dynamic(() => import('./lead-info-sheet'));
const UpdateInfoModal = dynamic(() => import('./update-info-modal'));

interface CallGroupCardProps {
  group: CallGroup;
  onUpdateContactNote: (callerId: string, newNote: string) => void;
  onUpdateCallNote: (callId: string, newNote: string) => void;
  onUpdateCallerInfo: (callerId: string, info: { custom_name?: string; caller_type?: string }) => void;
  onExcludeNumber: (callerId: string) => void;
  onMarkSynced: (callerId: string, currentStatus: boolean) => void;
  setCallsByPhone: React.Dispatch<React.SetStateAction<Record<string, Call[]>>>;
  isExpanded: boolean;
  onToggleExpand: () => void;
  currentlyPlaying: string | null;
  setCurrentlyPlaying: (id: string | null) => void;
}

const callTypeIcons: Record<Call['type'], React.ReactNode> = {
  incoming: <ArrowDownLeft className="h-4 w-4 text-green-500" />,
  outgoing: <ArrowUpRight className="h-4 w-4 text-blue-500" />,
  missed: <PhoneMissed className="h-4 w-4 text-red-500" />,
  rejected: <PhoneOff className="h-4 w-4 text-destructive" />,
};

function formatDuration(seconds: number) {
  if (seconds === 0) return '0s';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return [h > 0 ? `${h}h` : '', m > 0 ? `${m}m` : '', s > 0 ? `${s}s` : ''].filter(Boolean).join(' ');
}

function CallDetail({ 
    call, 
    onUpdateCallNote,
    currentlyPlaying,
    setCurrentlyPlaying,
}: { 
    call: Call; 
    onUpdateCallNote: (callId: string, newNote: string) => void;
    currentlyPlaying: string | null;
    setCurrentlyPlaying: (id: string | null) => void;
}) {
  const [note, setNote] = useState(call.note || '');

  useEffect(() => {
    setNote(call.note || '');
  }, [call.note]);

  const handleSaveNote = () => {
    onUpdateCallNote(call.id, note);
  };
  
  let recordingUrl = call.recording_url;
  if (recordingUrl) {
    try {
      const url = new URL(recordingUrl);
      const audioFile = url.searchParams.get('af');
      if (audioFile) {
        recordingUrl = audioFile;
      }
    } catch (e) {
      // Not a valid URL, use it as is
    }
  }


  return (
    <Accordion type="single" collapsible className="w-full">
      <AccordionItem value={call.id} className="border-0 overflow-hidden rounded-md">
        <AccordionTrigger className="flex w-full items-center justify-between text-xs p-2 hover:no-underline hover:bg-accent/50 rounded-md">
          <div className="flex items-center gap-2">
            {callTypeIcons[call.type]}
            <span className="capitalize">{call.type}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-muted-foreground">{format(new Date(call.created_ts), 'MMM d, h:mm a')}</span>
            <Badge variant="outline" className="hidden sm:inline-flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {formatDuration(call.duration)}
            </Badge>
          </div>
        </AccordionTrigger>
        <AccordionContent>
          <div className="space-y-2 pt-2 px-2 pb-2">
            <div className="sm:hidden pb-2">
              <Badge variant="outline" className="inline-flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {formatDuration(call.duration)}
              </Badge>
            </div>
            <Textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Add a note for this specific call..."
              className="min-h-[60px] text-xs"
            />
            <div className="flex gap-2">
              <Button onClick={handleSaveNote} size="sm" className="flex-1">
                Save Call Note
              </Button>
            </div>
            {recordingUrl && (
                <AudioPlayer 
                    src={recordingUrl} 
                    id={call.id}
                    currentlyPlaying={currentlyPlaying}
                    setCurrentlyPlaying={setCurrentlyPlaying}
                />
            )}
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}

function CallGroupCardComponent({ 
    group, 
    onUpdateContactNote, 
    onUpdateCallNote, 
    onUpdateCallerInfo,
    onExcludeNumber, 
    onMarkSynced,
    setCallsByPhone, 
    isExpanded, 
    onToggleExpand,
    currentlyPlaying,
    setCurrentlyPlaying,
}: CallGroupCardProps) {
  const { caller, calls } = group;
  const [contactNote, setContactNote] = useState(caller.note || '');
  const [isLoadingCalls, setIsLoadingCalls] = useState(false);
  const [isExcludeAlertOpen, setIsExcludeAlertOpen] = useState(false);
  const [isLeadInfoOpen, setIsLeadInfoOpen] = useState(false);
  const [isUpdateInfoOpen, setIsUpdateInfoOpen] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    setContactNote(caller.note || '');
  }, [caller.note]);

  const handleAccordionToggle = async (open: boolean) => {
    onToggleExpand();
    if (open && calls.length === 0) {
      setIsLoadingCalls(true);
      try {
        const fetchedCalls = await fetchCalls({ phone: caller.phone });
        setCallsByPhone(prev => ({ ...prev, [caller.phone]: fetchedCalls }));
      } catch (error) {
        toast({ variant: 'destructive', title: 'Error', description: 'Could not fetch call history.' });
      } finally {
        setIsLoadingCalls(false);
      }
    }
  };

  const handleSaveContactNote = () => {
    onUpdateContactNote(caller.id, contactNote);
  };
  
  const handleMarkSynced = () => {
      onMarkSynced(caller.id, caller.last_sync);
  }

  const handleExclude = () => {
    onExcludeNumber(caller.id);
    setIsExcludeAlertOpen(false);
  };
  
  const handleCopy = () => {
    navigator.clipboard.writeText(caller.phone);
    toast({ title: "Copied!", description: `${caller.phone} copied to clipboard.` });
  };
  
  const getCallerDisplay = () => {
    if (caller.lead_name) {
        const budget = caller.budget ? ` - ${caller.budget}` : '';
        return `${caller.lead_id || ''}. ${caller.lead_name}${budget}`;
    }
    if (caller.custom_name) {
        const type = caller.caller_type ? ` (${caller.caller_type})` : '';
        return `${caller.custom_name}${type}`;
    }
    return caller.phone;
  };

  const lastCallTime = formatDistanceToNow(new Date(caller.last_call), { addSuffix: true }).replace('about ', '');
  const notePreview = caller.note ? caller.note.split(' ').slice(0, 7).join(' ') + (caller.note.split(' ').length > 7 ? '...' : '') : '';
  const isPlayingInGroup = calls.some(c => c.id === currentlyPlaying);

  return (
    <Card className="overflow-hidden transition-all hover:shadow-md">
      <Accordion type="single" collapsible value={isExpanded ? caller.id : ''} onValueChange={(value) => handleAccordionToggle(!!value)}>
        <AccordionItem value={caller.id} className="border-b-0">
          <AccordionTrigger className="p-4 hover:no-underline [&[data-state=open]]:bg-accent">
            <div className="flex w-full items-start gap-4 text-left">
              <div className="flex-1 space-y-1">
                <div className="flex items-center gap-2">
                  {isPlayingInGroup && <Speaker className="h-4 w-4 text-primary animate-pulse" />}
                  {caller.last_sync && <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />}
                  <p className="font-semibold text-foreground">{getCallerDisplay()}</p>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  {callTypeIcons[caller.last_call_type]}
                  <span>{lastCallTime}</span>
                  {(caller.last_call_type !== 'missed' && caller.last_call_type !== 'rejected') && (
                    <>
                      <span className="text-xs">•</span>
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        <span>{formatDuration(caller.last_call_duration)}</span>
                      </div>
                    </>
                  )}
                </div>
                {notePreview && <p className="text-xs text-muted-foreground pt-1">{notePreview}</p>}
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="secondary">{caller.calls_in_range} {caller.calls_in_range > 1 ? 'calls' : 'call'}</Badge>
              </div>
            </div>
          </AccordionTrigger>
          <AccordionContent>
            <div className="space-y-4 pb-4">
              <div className="px-4 pt-2">
                <div className="max-h-60 overflow-y-auto space-y-1 rounded-lg border bg-background/50 p-1">
                  {isLoadingCalls ? (
                    <div className="flex justify-center items-center p-4">
                      <Loader2 className="h-6 w-6 animate-spin text-primary" />
                    </div>
                  ) : calls.length > 0 ? (
                    calls.map((call) => (
                        <CallDetail 
                            key={call.id} 
                            call={call} 
                            onUpdateCallNote={onUpdateCallNote}
                            currentlyPlaying={currentlyPlaying}
                            setCurrentlyPlaying={setCurrentlyPlaying}
                        />
                    ))
                  ) : (
                     <p className="p-4 text-center text-sm text-muted-foreground">No call history found.</p>
                  )}
                </div>
              </div>
              <div className="px-4">
                <div className="flex justify-between items-center mb-2">
                  <label htmlFor={`note-${caller.phone}`} className="block text-sm font-medium text-foreground">
                  Note for {caller.phone}
                  </label>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-8 w-8 -mr-2">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                       {caller.lead_id && (
                        <>
                          <DropdownMenuItem onClick={() => setIsLeadInfoOpen(true)}>
                            <Info className="mr-2 h-4 w-4" />
                            Show Lead Info
                          </DropdownMenuItem>
                          <DropdownMenuItem asChild>
                            <a href={`https://uptwn-sales2.netlify.app/leads/${caller.lead_id}`} target="_blank" rel="noopener noreferrer" className="flex items-center">
                              <ExternalLink className="mr-2 h-4 w-4" />
                              Go to Lead
                            </a>
                          </DropdownMenuItem>
                        </>
                       )}
                       <DropdownMenuItem onClick={() => setIsUpdateInfoOpen(true)}>
                          <Edit className="mr-2 h-4 w-4" />
                          Update Info
                        </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <a href={`tel:${caller.phone}`} className="flex items-center">
                          <Phone className="mr-2 h-4 w-4" />
                          Call
                        </a>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                         <a href={`https://wa.me/${caller.phone}`} target="_blank" rel="noopener noreferrer" className="flex items-center">
                          <MessageCircle className="mr-2 h-4 w-4" />
                           WhatsApp
                         </a>
                      </DropdownMenuItem>
                       <DropdownMenuItem onClick={handleCopy}>
                        <Copy className="mr-2 h-4 w-4" />
                        Copy Phone
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={() => setIsExcludeAlertOpen(true)} className="text-destructive">
                        <XCircle className="mr-2 h-4 w-4" />
                        Exclude
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
                <Textarea
                  id={`note-${caller.phone}`}
                  value={contactNote}
                  onChange={(e) => setContactNote(e.target.value)}
                  placeholder="Add a persistent note for this contact..."
                  className="min-h-[80px]"
                />
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button onClick={handleSaveContactNote} className="flex-1">
                    Save Contact Note
                  </Button>
                  <Button onClick={handleMarkSynced} className="flex-1" variant={caller.last_sync ? "default" : "secondary"}>
                    <CheckCircle2 className="mr-2 h-4 w-4" />
                    {caller.last_sync ? 'Undone' : 'Done'}
                  </Button>
                </div>

                {caller.segment && (
                  <div className="mt-4 rounded-md border bg-muted/50 p-3 text-sm">
                    <p className="font-semibold">{caller.segment}</p>
                  </div>
                )}
              </div>
            </div>
          </AccordionContent>
        </AccordionItem>
      </Accordion>
       <AlertDialog open={isExcludeAlertOpen} onOpenChange={setIsExcludeAlertOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently exclude the number <span className="font-semibold text-foreground">{caller.phone}</span> from your call log. You cannot undo this action.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleExclude} className="bg-destructive hover:bg-destructive/90">
              Exclude
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      {isLeadInfoOpen && caller.lead_id && (
        <LeadInfoSheet 
            leadId={caller.lead_id} 
            isOpen={isLeadInfoOpen} 
            onClose={() => setIsLeadInfoOpen(false)} 
        />
      )}
       {isUpdateInfoOpen && (
          <UpdateInfoModal
            isOpen={isUpdateInfoOpen}
            onClose={() => setIsUpdateInfoOpen(false)}
            caller={caller}
            onUpdate={onUpdateCallerInfo}
          />
       )}
    </Card>
  );
}

export const CallGroupCard = React.memo(CallGroupCardComponent);
    
