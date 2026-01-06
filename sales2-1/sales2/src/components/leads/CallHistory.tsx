import React, { useEffect, useState } from 'react';
import { ArrowUpRight, ArrowDownLeft, Clock, ChevronDown, ChevronUp } from 'lucide-react';
import { CallLog, CallerDetailsResponse } from '../../types';
import { format } from 'date-fns';
import AudioPlayer from '../common/AudioPlayer';

interface CallHistoryProps {
    phone: string;
    onTitleChange?: (title: string) => void;
}

const MatchAccordion: React.FC<{
    title: React.ReactNode;
    children: React.ReactNode;
    defaultOpen?: boolean;
    count?: number;
    showCount?: boolean;
}> = ({ title, children, defaultOpen = false, count, showCount = true }) => {
    const [isOpen, setIsOpen] = useState(defaultOpen);

    return (
        <div className="border border-gray-200 rounded-lg overflow-hidden bg-white">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 transition-colors text-left"
            >
                <div className="font-medium text-gray-800 flex items-center gap-2">
                    {title}
                    {showCount && count !== undefined && count > 0 && (
                        <span className="text-xs bg-gray-200 text-gray-600 px-2 py-0.5 rounded-full">
                            {count}
                        </span>
                    )}
                </div>
                {isOpen ? <ChevronUp size={18} className="text-gray-500" /> : <ChevronDown size={18} className="text-gray-500" />}
            </button>
            {isOpen && <div className="p-3 border-t border-gray-200">{children}</div>}
        </div>
    );
};

const CallHistory: React.FC<CallHistoryProps> = ({ phone, onTitleChange }) => {
    const [calls, setCalls] = useState<CallLog[]>([]);
    const [callerDetails, setCallerDetails] = useState<CallerDetailsResponse | null>(null);
    const [loadingCalls, setLoadingCalls] = useState(false);
    const [loadingDetails, setLoadingDetails] = useState(false);
    const [errorCalls, setErrorCalls] = useState<string | null>(null);
    const [errorDetails, setErrorDetails] = useState<string | null>(null);
    const [playingId, setPlayingId] = useState<string | null>(null);
    const [showCallLogs, setShowCallLogs] = useState(false);

    // Helper to construct the header string
    const getHeaderString = () => {
        if (!callerDetails?.matches) return 'Related Data';

        const parts = [];

        const callersCount = callerDetails.matches.callers?.length || 0;
        if (callersCount > 0) {
            parts.push(callersCount > 1 ? `Calls (${callersCount})` : 'Calls');
        }

        const propertiesCount = callerDetails.matches.gov_properties?.length || 0;
        if (propertiesCount > 0) {
            parts.push(propertiesCount > 1 ? `Properties (${propertiesCount})` : 'Properties');
        }

        const oldListingCount = callerDetails.matches.v3_person?.length || 0;
        if (oldListingCount > 0) {
            parts.push(oldListingCount > 1 ? `Old Listing (${oldListingCount})` : 'Old Listing');
        }

        return parts.join(', ');
    };

    useEffect(() => {
        if (onTitleChange) {
            onTitleChange(getHeaderString());
        }
    }, [callerDetails, onTitleChange]);

    useEffect(() => {
        const fetchCallerDetails = async () => {
            if (!phone) return;
            const sanitizedPhone = phone.replace(/\D/g, '').slice(-10);

            setLoadingDetails(true);
            setErrorDetails(null);
            try {
                const response = await fetch(`https://prop.digiheadway.in/api/calls/caller_id2.php?phone=${sanitizedPhone}`);
                const data = await response.json();
                setCallerDetails(data);
            } catch (err) {
                console.error('Error fetching caller details:', err);
                setErrorDetails('Failed to load caller details');
            } finally {
                setLoadingDetails(false);
            }
        };

        fetchCallerDetails();
    }, [phone]);

    const fetchCalls = async () => {
        if (!phone || calls.length > 0) return; // Don't refetch if already loaded

        const sanitizedPhone = phone.replace(/\D/g, '').slice(-10);

        setLoadingCalls(true);
        setErrorCalls(null);
        try {
            const response = await fetch(`https://prop.digiheadway.in/api/calls/crm.php?action=fetch_calls&phone=${sanitizedPhone}`);
            const data = await response.json();

            if (data.status === 'success') {
                setCalls(data.data);
            } else {
                setCalls([]);
            }
        } catch (err) {
            console.error('Error fetching call history:', err);
            setErrorCalls('Failed to load call history');
        } finally {
            setLoadingCalls(false);
        }
    };

    const handleToggleCallLogs = () => {
        if (!showCallLogs) {
            fetchCalls();
        }
        setShowCallLogs(!showCallLogs);
    };

    if (loadingDetails) return (
        <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
    );

    const callerMatch = callerDetails?.matches?.callers?.[0];

    return (
        <div className="space-y-4">
            {/* Main Container */}
            {callerDetails && (
                <div className="rounded-lg  overflow-hidden">
                    <div className="space-y-4">
                        {/* Note */}
                        {callerDetails.note && (
                            <div className="text-gray-700 text-sm pb-4 border-b border-gray-100">
                                {callerDetails.note}
                            </div>
                        )}

                        {/* Last Call Detail (from first caller match) */}
                        {callerMatch && (
                            <div className="text-sm space-y-1">
                                <div className="flex items-center justify-between text-gray-600">
                                    <span>Last Call:</span>
                                    <span className="font-medium text-gray-900">
                                        {callerMatch.last_call ? format(new Date(callerMatch.last_call), 'd MMM | h a') : 'N/A'}
                                    </span>
                                </div>
                                <div className="flex items-center justify-between text-gray-600">
                                    <span>Type:</span>
                                    <span className="font-medium text-gray-900 capitalize">{callerMatch.last_call_type}</span>
                                </div>
                                <div className="flex items-center justify-between text-gray-600">
                                    <span>Duration:</span>
                                    <span className="font-medium text-gray-900">{callerMatch.last_call_duration}s</span>
                                </div>
                                <div className="flex items-center justify-between text-gray-600">
                                    <span>Total Calls:</span>
                                    <span className="font-medium text-gray-900">{callerMatch.calls}</span>
                                </div>
                            </div>
                        )}

                        {/* Call History Accordion */}
                        <div className="border border-gray-200 rounded-lg overflow-hidden bg-white">
                            <button
                                onClick={handleToggleCallLogs}
                                className="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 transition-colors text-left"
                            >
                                <span className="font-medium text-gray-800 flex items-center gap-2">
                                    <Clock size={16} />
                                    Call History
                                </span>
                                {showCallLogs ? <ChevronUp size={18} className="text-gray-500" /> : <ChevronDown size={18} className="text-gray-500" />}
                            </button>

                            {showCallLogs && (
                                <div className="p-3 border-t border-gray-200 animate-fade-in">
                                    {loadingCalls ? (
                                        <div className="flex justify-center py-4">
                                            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                                        </div>
                                    ) : errorCalls ? (
                                        <div className="text-red-500 text-sm py-2">{errorCalls}</div>
                                    ) : calls.length === 0 ? (
                                        <div className="text-gray-500 text-sm py-2">No call history found.</div>
                                    ) : (
                                        <div className="space-y-2">
                                            {calls.map((call) => {
                                                const recordingUrl = call.recording_url || call.recording;
                                                return (
                                                    <div key={call.id} className="flex flex-col sm:flex-row sm:items-center justify-between p-2 bg-gray-50 rounded border border-gray-100 text-sm gap-2 sm:gap-0">
                                                        <div className="flex items-center gap-2">
                                                            <div className={`p-1.5 rounded-full ${call.type === 'outgoing' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'}`}>
                                                                {call.type === 'outgoing' ? <ArrowUpRight size={12} /> : <ArrowDownLeft size={12} />}
                                                            </div>
                                                            <div>
                                                                <div className="font-medium text-gray-900">{format(new Date(call.created_ts), 'dd MMM, h:mm a')}</div>
                                                                <div className="text-xs text-gray-500">{call.duration}s</div>
                                                            </div>
                                                        </div>

                                                        {recordingUrl && (
                                                            <div className="w-full sm:w-auto mt-2 sm:mt-0 sm:ml-4">
                                                                <AudioPlayer
                                                                    src={recordingUrl}
                                                                    isPlayingProp={playingId === call.id}
                                                                    onPlay={() => setPlayingId(call.id)}
                                                                    onPause={() => {
                                                                        if (playingId === call.id) setPlayingId(null);
                                                                    }}
                                                                />
                                                            </div>
                                                        )}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* V3 Person Matches */}
                        {callerDetails.matches?.v3_person?.length > 0 && (
                            <MatchAccordion
                                title="Old Listing"
                                count={callerDetails.matches.v3_person.length}
                            >
                                <div className="space-y-3">
                                    {callerDetails.matches.v3_person.map((person) => (
                                        <div key={person.id} className="text-sm">
                                            <div className="font-semibold text-gray-800">{person.name}</div>
                                            <div className="text-gray-600">{person.phone}</div>
                                            <div className="text-blue-600 text-xs mt-0.5">{person.source}</div>
                                            {person.note && (
                                                <div className="mt-1 text-gray-600 italic">
                                                    "{person.note}"
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </MatchAccordion>
                        )}

                        {/* Network Properties Matches */}
                        {callerDetails.matches?.gov_properties?.length > 0 && (
                            <MatchAccordion
                                title="Properties"
                                count={callerDetails.matches.gov_properties.length}
                            >
                                <div className="space-y-3">
                                    {callerDetails.matches.gov_properties.map((prop) => (
                                        <div key={prop.id} className="text-sm border-b border-gray-100 last:border-0 pb-2 last:pb-0">
                                            <div className="font-semibold text-gray-800">{prop.OwnerName}</div>
                                            <div className="text-gray-600 mt-0.5">{prop.Address1}</div>
                                            <div className="flex gap-3 mt-1 text-xs text-gray-500">
                                                <span>PID: {prop.PID}</span>
                                                <span>Size: {prop.PlotSize} {prop.Unit}</span>
                                            </div>
                                            {prop.remark && (
                                                <div className="mt-1 text-gray-600 italic">
                                                    "{prop.remark}"
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </MatchAccordion>
                        )}
                    </div>
                </div>
            )}

            {errorDetails && <div className="text-red-500 text-sm">{errorDetails}</div>}
        </div>
    );
};

export default CallHistory;
