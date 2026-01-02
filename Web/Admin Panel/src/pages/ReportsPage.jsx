import { useState, useEffect } from 'react';
import api from '../api/client';
import { format } from 'date-fns';
import DateRangeFilter from '../components/DateRangeFilter';
import EmployeeDropdown from '../components/EmployeeDropdown';
import {
    BarChart2,
    Calendar,
    Download,
    Phone,
    Clock,
    CheckCircle,
    XCircle,
    TrendingUp,
    AlertCircle,
    Smartphone,
    Activity,
    Wifi,
    WifiOff,
    PhoneMissed,
    ArrowRight
} from 'lucide-react';

export default function ReportsPage() {
    const [loading, setLoading] = useState(true);
    const [reportData, setReportData] = useState({
        summary: {},
        employee_performance: [],
        missed_opportunities: [],
        operational_insights: {
            activity_heatmap: [],
            device_health: []
        }
    });
    const [dateRange, setDateRange] = useState('7days');
    const [customRange, setCustomRange] = useState({ startDate: '', endDate: '' });
    const [selectedEmployee, setSelectedEmployee] = useState('');
    const [employees, setEmployees] = useState([]);

    useEffect(() => {
        fetchEmployees();
    }, []);

    useEffect(() => {
        fetchReports();
    }, [dateRange, customRange, selectedEmployee]);

    const fetchEmployees = async () => {
        try {
            const res = await api.get('/employees.php');
            if (res.data && Array.isArray(res.data)) {
                setEmployees(res.data);
            }
        } catch (err) {
            console.error("Failed to fetch employees", err);
        }
    };

    const fetchReports = async () => {
        // Don't fetch if custom range is selected but incomplete
        if (dateRange === 'custom' && (!customRange.startDate || !customRange.endDate)) {
            return;
        }

        setLoading(true);
        try {
            const params = new URLSearchParams({
                type: 'all', // Fetch all reports at once
                dateRange,
                tzOffset: new Date().getTimezoneOffset(),
                employeeId: selectedEmployee || 'all'
            });

            if (dateRange === 'custom') {
                params.append('startDate', customRange.startDate);
                params.append('endDate', customRange.endDate);
            }

            const res = await api.get(`/reports.php?${params.toString()}`);

            if (res.data) {
                setReportData(res.data);
            }
        } catch (err) {
            console.error("Failed to fetch reports", err);
        } finally {
            setLoading(false);
        }
    };

    const stats = [
        { label: 'Total Calls', value: reportData.summary?.total_calls || 0, icon: Phone, color: 'text-blue-600', bg: 'bg-blue-50' },
        { label: 'Total Duration', value: reportData.summary?.formatted_duration || '0m', icon: Clock, color: 'text-orange-600', bg: 'bg-orange-50' },
        { label: 'Connected', value: reportData.summary?.connected || 0, icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50' },
        { label: 'Missed', value: reportData.summary?.missed || 0, icon: XCircle, color: 'text-red-600', bg: 'bg-red-50' },
    ];

    // Helper for Heatmap
    const getHeatmapIntensity = (count) => {
        if (!count) return 'bg-gray-50';
        if (count < 5) return 'bg-blue-100';
        if (count < 10) return 'bg-blue-300';
        if (count < 20) return 'bg-blue-500';
        return 'bg-blue-700';
    };

    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const hours = Array.from({ length: 24 }, (_, i) => i);

    return (
        <div className="relative pb-20">
            {/* Floating Loading Indicator */}
            {loading && (
                <div className="absolute inset-x-0 top-12 z-20 flex justify-center pointer-events-none">
                    <div className="bg-white shadow-xl border px-4 py-2 rounded-full flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                        <span className="text-sm font-medium text-gray-600">Updating...</span>
                    </div>
                </div>
            )}

            <div className={`space-y-8 ${loading ? 'opacity-60 transition-opacity duration-200' : ''}`}>
                <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight">Analytics & Reports</h1>
                        <p className="text-gray-500 text-sm mt-1">Comprehensive insights into call performance and team productivity.</p>
                    </div>

                    <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 w-full lg:w-auto">
                        <DateRangeFilter
                            value={dateRange}
                            onChange={setDateRange}
                            customRange={customRange}
                            onCustomRangeChange={setCustomRange}
                        />

                        <EmployeeDropdown
                            value={selectedEmployee}
                            onChange={setSelectedEmployee}
                            employees={employees}
                        />
                    </div>
                </div>

                {/* Overview Stats */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    {stats.map((stat) => (
                        <div key={stat.label} className="card p-4 flex items-center gap-4 hover:shadow-md transition-all">
                            <div className={`${stat.bg} ${stat.color} p-3 rounded-xl`}>
                                <stat.icon size={24} />
                            </div>
                            <div>
                                <p className="text-sm font-medium text-gray-500">{stat.label}</p>
                                <h3 className="text-2xl font-bold mt-0.5">{stat.value}</h3>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Report 1: Employee Performance */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold flex items-center gap-2">
                            <TrendingUp size={20} className="text-blue-600" />
                            1. Employee Performance
                        </h2>
                    </div>

                    <div className="card !p-0 overflow-hidden border border-gray-200">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 font-medium">
                                    <tr>
                                        <th className="px-6 py-4">Employee</th>
                                        <th className="px-6 py-4 text-right">Total Calls</th>
                                        <th className="px-6 py-4 text-right">In / Out</th>
                                        <th className="px-6 py-4 text-right">Connection Rate</th>
                                        <th className="px-6 py-4 text-right">Total Talk Time</th>
                                        <th className="px-6 py-4 text-right">Avg Handle Time</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {reportData.employee_performance?.map((emp) => (
                                        <tr key={emp.employee_id} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4 font-medium text-gray-900">{emp.name}</td>
                                            <td className="px-6 py-4 text-right font-semibold">{emp.total_calls}</td>
                                            <td className="px-6 py-4 text-right">
                                                <span className="text-blue-600">{emp.inbound}</span> / <span className="text-green-600">{emp.outbound}</span>
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <div className="flex items-center justify-end gap-2">
                                                    <span className={`font-medium ${emp.connection_rate > 30 ? 'text-green-600' : 'text-gray-600'}`}>
                                                        {emp.connection_rate}%
                                                    </span>
                                                    <span className="text-xs text-gray-400">({emp.outbound_connected}/{emp.outbound})</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 text-right font-mono text-xs">{emp.formatted_duration}</td>
                                            <td className="px-6 py-4 text-right font-mono text-xs text-gray-500">{emp.formatted_aht}</td>
                                        </tr>
                                    ))}
                                    {(!reportData.employee_performance || reportData.employee_performance.length === 0) && (
                                        <tr><td colSpan="6" className="p-8 text-center text-gray-500">No data available</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                {/* Report 2: Missed Opportunities */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold flex items-center gap-2">
                            <PhoneMissed size={20} className="text-red-500" />
                            2. Missed Opportunities
                        </h2>
                    </div>

                    <div className="card !p-0 overflow-hidden border border-gray-200">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 font-medium">
                                    <tr>
                                        <th className="px-6 py-4">Caller</th>
                                        <th className="px-6 py-4">Missed At</th>
                                        <th className="px-6 py-4">Routed To</th>
                                        <th className="px-6 py-4">Status</th>
                                        <th className="px-6 py-4 text-right">Action</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {reportData.missed_opportunities?.map((missed) => (
                                        <tr key={missed.id} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4">
                                                <div className="font-medium text-gray-900">{missed.name}</div>
                                                <div className="text-xs text-gray-500">{missed.phone}</div>
                                            </td>
                                            <td className="px-6 py-4 text-gray-600">{missed.missed_at_formatted}</td>
                                            <td className="px-6 py-4 text-gray-600">{missed.originally_routed_to}</td>
                                            <td className="px-6 py-4">
                                                {missed.status === 'Returned' ? (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                                                        Returned
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                                                        Unreturned
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <button className="text-blue-600 hover:text-blue-800 text-xs font-medium">
                                                    Call Back
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    {(!reportData.missed_opportunities || reportData.missed_opportunities.length === 0) && (
                                        <tr><td colSpan="5" className="p-8 text-center text-gray-500">No missed calls in this period</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                {/* Report 5: Operational Insights */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold flex items-center gap-2">
                            <Activity size={20} className="text-purple-600" />
                            5. Operational Insights
                        </h2>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Device Health */}
                        <div className="card p-4 lg:col-span-1">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="font-medium flex items-center gap-2 text-gray-700">
                                    <Smartphone size={18} />
                                    Device Health
                                </h3>
                                <span className="text-xs text-gray-400">Last Sync Status</span>
                            </div>
                            <div className="space-y-3">
                                {reportData.operational_insights?.device_health?.map((dev, idx) => (
                                    <div key={idx} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50 border border-transparent hover:border-gray-100">
                                        <div className="flex items-center gap-3">
                                            <div className={`p-2 rounded-full ${dev.status === 'Online' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'}`}>
                                                {dev.status === 'Online' ? <Wifi size={16} /> : <WifiOff size={16} />}
                                            </div>
                                            <div>
                                                <div className="font-medium text-sm text-gray-900">{dev.name}</div>
                                                <div className="text-xs text-gray-500">{dev.last_active}</div>
                                            </div>
                                        </div>
                                        <span className={`text-xs font-medium px-2 py-1 rounded-full ${dev.status === 'Online' ? 'bg-green-50 text-green-700' :
                                            dev.status === 'Away' ? 'bg-yellow-50 text-yellow-700' :
                                                'bg-red-50 text-red-700'
                                            }`}>
                                            {dev.status}
                                        </span>
                                    </div>
                                ))}
                                {(!reportData.operational_insights?.device_health || reportData.operational_insights.device_health.length === 0) && (
                                    <p className="text-center text-gray-400 text-sm py-4">No active devices found</p>
                                )}
                            </div>
                        </div>

                        {/* Activity Heatmap */}
                        <div className="card p-4 lg:col-span-2">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="font-medium flex items-center gap-2 text-gray-700">
                                    <Clock size={18} />
                                    Peak Activity Times
                                </h3>
                                <div className="flex gap-2 items-center text-xs text-gray-500">
                                    <span>Less</span>
                                    <div className="w-3 h-3 bg-blue-100 rounded"></div>
                                    <div className="w-3 h-3 bg-blue-300 rounded"></div>
                                    <div className="w-3 h-3 bg-blue-500 rounded"></div>
                                    <div className="w-3 h-3 bg-blue-700 rounded"></div>
                                    <span>More</span>
                                </div>
                            </div>

                            <div className="overflow-x-auto">
                                <div className="min-w-[500px]">
                                    {/* Header Row (Hours) */}
                                    <div className="flex mb-1">
                                        <div className="w-12"></div> {/* Spacer for day labels */}
                                        {hours.map(h => (
                                            <div key={h} className="flex-1 text-[10px] text-center text-gray-400">
                                                {h}
                                            </div>
                                        ))}
                                    </div>

                                    {/* Grid */}
                                    <div className="flex flex-col gap-1">
                                        {days.map((dayName, dayIndex) => (
                                            <div key={dayName} className="flex items-center">
                                                <div className="w-12 text-xs font-medium text-gray-500">{dayName}</div>
                                                <div className="flex-1 grid gap-1" style={{ gridTemplateColumns: 'repeat(24, minmax(0, 1fr))' }}>
                                                    {hours.map(hour => {
                                                        // Find data for this day (dayIndex + 1 because SQL returns 1-based Sunday) and hour
                                                        // Note: JS dayIndex 0 = Sunday if we map directly? 
                                                        // SQL DAYOFWEEK: 1=Sunday, 7=Saturday. 
                                                        // My array days starts with Sun, so index 0 corresponds to SQL 1.
                                                        const sqlDay = dayIndex + 1;
                                                        const cellData = reportData.operational_insights?.activity_heatmap?.find(
                                                            d => parseInt(d.day_num) === sqlDay && parseInt(d.hour_num) === hour
                                                        );
                                                        const count = cellData ? parseInt(cellData.call_volume) : 0;

                                                        return (
                                                            <div
                                                                key={hour}
                                                                className={`h-6 rounded-sm transition-colors ${getHeatmapIntensity(count)}`}
                                                                title={`${dayName} ${hour}:00 - ${count} calls`}
                                                            ></div>
                                                        );
                                                    })}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
