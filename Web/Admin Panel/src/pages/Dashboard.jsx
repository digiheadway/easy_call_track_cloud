import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { format, parseISO } from 'date-fns';
import { usePersonModal } from '../context/PersonModalContext';
import DateRangeFilter from '../components/DateRangeFilter';
import EmployeeDropdown from '../components/EmployeeDropdown';
import {
    PhoneCall,
    Users,
    Clock,
    CheckCircle2,
    XCircle,
    BarChart3,
    ExternalLink,
    Play,
    Pause
} from 'lucide-react';
import { useAudioPlayer } from '../context/AudioPlayerContext';

export default function Dashboard() {
    const navigate = useNavigate();
    const { openPersonModal } = usePersonModal();
    const { playRecording, currentCall, isPlaying, currentTime, duration: activeDuration } = useAudioPlayer();
    const [data, setData] = useState({
        metrics: {},
        breakdown: [],
        recent_activities: [],
        employees: []
    });
    const [dateRange, setDateRange] = useState('7days');
    const [customRange, setCustomRange] = useState({ startDate: '', endDate: '' });
    const [selectedEmployee, setSelectedEmployee] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardData();
    }, [dateRange, selectedEmployee, customRange]);

    const fetchDashboardData = async () => {
        // Don't fetch if custom range is selected but incomplete
        if (dateRange === 'custom' && (!customRange.startDate || !customRange.endDate)) {
            return;
        }

        setLoading(true);
        try {
            const params = new URLSearchParams({
                dateRange,
                employeeId: selectedEmployee || 'all',
                tzOffset: new Date().getTimezoneOffset()
            });

            if (dateRange === 'custom') {
                params.append('startDate', customRange.startDate);
                params.append('endDate', customRange.endDate);
            }

            const res = await api.get(`/dashboard.php?${params.toString()}`);
            if (res.data) {
                setData(res.data);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const statCards = [
        { label: 'Total Calls', value: data.metrics.total_calls || 0, icon: PhoneCall, color: 'text-blue-600', bg: 'bg-blue-50' },
        { label: 'Total Persons', value: data.metrics.total_persons || 0, icon: Users, color: 'text-purple-600', bg: 'bg-purple-50' },
        { label: 'Total Duration', value: data.metrics.formatted_duration || '0m 0s', icon: Clock, color: 'text-orange-600', bg: 'bg-orange-50' },
        { label: 'Connected', value: data.metrics.connected || 0, icon: CheckCircle2, color: 'text-green-600', bg: 'bg-green-50' },
        { label: 'Not Connected', value: data.metrics.not_connected || 0, icon: XCircle, color: 'text-red-600', bg: 'bg-red-50' },
    ];

    if (loading && !data.employees.length) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <div className="relative">
            {loading && data.employees.length > 0 && (
                <div className="absolute inset-x-0 top-20 z-10 flex justify-center pointer-events-none">
                    <div className="bg-white shadow-md border px-4 py-2 rounded-full flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                        <span className="text-sm font-medium text-gray-600">Updating...</span>
                    </div>
                </div>
            )}

            <div className={`space-y-8 ${loading ? 'opacity-70 transition-opacity duration-200' : ''}`}>
                {/* Header with Filters */}
                <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                    {/* Date Range Chips */}
                    <DateRangeFilter
                        value={dateRange}
                        onChange={setDateRange}
                        customRange={customRange}
                        onCustomRangeChange={setCustomRange}
                    />

                    {/* Employee Filter */}
                    <EmployeeDropdown
                        value={selectedEmployee}
                        onChange={setSelectedEmployee}
                        employees={data.employees}
                    />
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
                    {statCards.map((card) => (
                        <div key={card.label} className="card p-4 flex flex-col items-start gap-3 hover:shadow-md transition-shadow">
                            <div className={`${card.bg} ${card.color} p-2 rounded-lg`}>
                                <card.icon size={20} />
                            </div>
                            <div>
                                <p className="text-sm font-medium text-gray-500">{card.label}</p>
                                <h3 className="text-xl font-bold mt-0.5">{card.value}</h3>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Breakdown Table */}
                <div className="space-y-4">
                    <div className="flex items-center gap-2">
                        <BarChart3 size={18} className="text-gray-500" />
                        <h2 className="text-lg font-semibold">Daily Breakdown</h2>
                    </div>
                    <div className="card !p-0 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-[#f8fafc] border-b border-gray-100 text-gray-500 font-medium">
                                    <tr>
                                        <th className="px-6 py-3">Date</th>
                                        <th className="px-6 py-3 text-right">Calls</th>
                                        <th className="px-6 py-3 text-right">Duration</th>
                                        <th className="px-6 py-3 text-right">Connected</th>
                                        <th className="px-6 py-3 text-right">Missed</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {data.breakdown.map((row, idx) => (
                                        <tr
                                            key={idx}
                                            className="hover:bg-gray-50 cursor-pointer transition-colors"
                                            onClick={() => navigate('/calls', { state: { date: row.date } })}
                                        >
                                            <td className="px-6 py-3 font-medium text-gray-900">
                                                {format(parseISO(row.date), 'MMM d, yyyy')}
                                            </td>
                                            <td className="px-6 py-3 text-right">{row.total_calls}</td>
                                            <td className="px-6 py-3 text-right font-mono text-xs">{row.formatted_duration}</td>
                                            <td className="px-6 py-3 text-right text-green-600">{row.connected}</td>
                                            <td className="px-6 py-3 text-right text-red-600">{row.not_connected}</td>
                                        </tr>
                                    ))}
                                    {data.breakdown.length === 0 && (
                                        <tr><td colSpan="5" className="p-8 text-center text-gray-500">No data for selected range</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold">Recent Activity</h2>
                    </div>
                    <div className="card !p-0 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-[#f8fafc] border-b border-gray-100 text-gray-500 font-medium">
                                    <tr>
                                        <th className="px-6 py-4">Date & Time</th>
                                        <th className="px-6 py-4">Employee</th>
                                        <th className="px-6 py-4">Contact</th>
                                        <th className="px-6 py-4">Type</th>
                                        <th className="px-6 py-4">Duration</th>
                                        <th className="px-6 py-4">Status</th>
                                        <th className="px-6 py-4">Recording</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {data.recent_activities.map((call) => (
                                        <tr key={call.id} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4 whitespace-nowrap text-gray-600">
                                                {format(new Date(call.call_time + (call.call_time.endsWith('Z') ? '' : 'Z')), 'MMM d, h:mm a')}
                                            </td>
                                            <td className="px-6 py-4 font-medium">{call.employee_name || 'System'}</td>
                                            <td className="px-6 py-4">
                                                <button
                                                    onClick={() => openPersonModal(call)}
                                                    className="text-left group/contact hover:bg-blue-50 -m-2 p-2 rounded-lg transition-colors"
                                                >
                                                    <div className="font-medium text-gray-900 group-hover/contact:text-blue-600 flex items-center gap-1">
                                                        {call.contact_name || call.phone_number}
                                                        <ExternalLink size={12} className="opacity-0 group-hover/contact:opacity-100 transition-opacity" />
                                                    </div>
                                                </button>
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className={
                                                    call.type === 'Missed' || call.type?.toLowerCase().includes('reject') ? 'text-red-500' :
                                                        call.type?.toLowerCase().includes('block') ? 'text-gray-500' : 'text-gray-500'
                                                }>{call.type}</span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {call.duration >= 60
                                                    ? `${Math.floor(call.duration / 60)}m ${call.duration % 60}s`
                                                    : `${call.duration}s`
                                                }
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className={`px-2 py-1 rounded-full text-xs font-medium ${call.duration > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                                                    }`}>
                                                    {call.duration > 0 ? 'Connected' :
                                                        call.type?.toLowerCase().includes('reject') ? 'Rejected' :
                                                            call.type?.toLowerCase().includes('block') ? 'Blocked' : 'Missed'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {call.recording_url ? (
                                                    <button
                                                        onClick={() => playRecording(call)}
                                                        className="flex items-center gap-2 bg-white hover:bg-blue-50 border border-gray-200 hover:border-blue-200 px-3 py-1.5 rounded-full shadow-sm transition-all group/player"
                                                        title="Play Recording"
                                                    >
                                                        <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-colors ${currentCall?.id === call.id ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600 group-hover/player:bg-blue-600 group-hover/player:text-white'}`}>
                                                            {currentCall?.id === call.id && isPlaying ? <Pause size={10} fill="currentColor" /> : <Play size={10} fill="currentColor" className={currentCall?.id === call.id ? '' : 'ml-0.5'} />}
                                                        </div>
                                                        <span className={`text-[10px] font-mono group-hover/player:text-blue-700 ${currentCall?.id === call.id ? 'text-blue-700 font-semibold' : 'text-gray-600'}`}>
                                                            {currentCall?.id === call.id ? (
                                                                `${Math.floor(currentTime / 60)}:${String(Math.floor(currentTime % 60)).padStart(2, '0')}`
                                                            ) : (
                                                                call.duration > 0 ? (
                                                                    call.duration >= 60 ? `${Math.floor(call.duration / 60)}:${String(call.duration % 60).padStart(2, '0')}` : `0:${String(call.duration).padStart(2, '0')}`
                                                                ) : '0:00'
                                                            )}
                                                        </span>
                                                    </button>
                                                ) : (
                                                    <span className="text-xs text-gray-400">No Rec</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                    {data.recent_activities.length === 0 && (
                                        <tr><td colSpan="7" className="p-8 text-center text-gray-500">No recent activity</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
