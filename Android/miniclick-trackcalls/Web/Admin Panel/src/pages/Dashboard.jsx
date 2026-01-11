import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { format, parseISO } from 'date-fns';
import DateRangeFilter from '../components/DateRangeFilter';
import EmployeeDropdown from '../components/EmployeeDropdown';
import {
    PhoneCall,
    Users,
    Clock,
    CheckCircle2,
    XCircle,
    BarChart3
} from 'lucide-react';

export default function Dashboard() {
    const navigate = useNavigate();
    const [data, setData] = useState({
        metrics: {},
        breakdown: [],
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
        {
            label: 'Total Calls',
            value: data.metrics.total_calls || 0,
            icon: PhoneCall,
            color: 'text-blue-600',
            bg: 'bg-blue-50 ',
            onClick: () => navigate('/calls', {
                state: {
                    dateRange,
                    customRange,
                    selectedEmployee
                }
            })
        },
        {
            label: 'Total Persons',
            value: data.metrics.total_persons || 0,
            icon: Users,
            color: 'text-purple-600',
            bg: 'bg-purple-50 ',
            onClick: () => navigate('/calls', {
                state: {
                    dateRange,
                    customRange,
                    selectedEmployee
                }
            })
        },
        {
            label: 'Total Duration',
            value: data.metrics.formatted_duration || '0m 0s',
            icon: Clock,
            color: 'text-orange-600',
            bg: 'bg-orange-50 ',
            onClick: () => navigate('/calls', {
                state: {
                    dateRange,
                    customRange,
                    selectedEmployee,
                    sortConfig: { key: 'duration', direction: 'DESC' }
                }
            })
        },
        {
            label: 'Connected',
            value: data.metrics.connected || 0,
            icon: CheckCircle2,
            color: 'text-green-600',
            bg: 'bg-green-50 ',
            onClick: () => navigate('/calls', {
                state: {
                    dateRange,
                    customRange,
                    selectedEmployee,
                    connectedFilter: 'connected'
                }
            })
        },
        {
            label: 'Not Connected',
            value: data.metrics.not_connected || 0,
            icon: XCircle,
            color: 'text-red-600',
            bg: 'bg-red-50 ',
            onClick: () => navigate('/calls', {
                state: {
                    dateRange,
                    customRange,
                    selectedEmployee,
                    connectedFilter: 'not_connected'
                }
            })
        },
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
                    <div className="bg-white  shadow-md border border-gray-200  px-4 py-2 rounded-full flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                        <span className="text-sm font-medium text-gray-600 ">Updating...</span>
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
                        <div
                            key={card.label}
                            className="card p-4 flex flex-col items-start gap-3 hover:shadow-md transition-shadow cursor-pointer group"
                            onClick={card.onClick}
                        >
                            <div className={`${card.bg} ${card.color} p-2.5 rounded-lg group-hover:scale-110 transition-transform`}>
                                <card.icon size={20} />
                            </div>
                            <div>
                                <p className="text-sm font-medium text-gray-500 ">{card.label}</p>
                                <h3 className="text-xl font-bold mt-0.5 text-gray-900 ">{card.value}</h3>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Breakdown Table */}
                <div className="space-y-4">
                    <div className="flex items-center gap-2">
                        <BarChart3 size={18} className="text-gray-500 " />
                        <h2 className="text-lg font-semibold ">Daily Breakdown</h2>
                    </div>
                    <div className="card !p-0 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50  border-b border-gray-200  text-gray-500  font-medium">
                                    <tr>
                                        <th className="px-6 py-3">Date</th>
                                        <th className="px-6 py-3 text-right">Calls</th>
                                        <th className="px-6 py-3 text-right">Duration</th>
                                        <th className="px-6 py-3 text-right">Connected</th>
                                        <th className="px-6 py-3 text-right">Missed</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100 ">
                                    {data.breakdown.map((row, idx) => (
                                        <tr
                                            key={idx}
                                            className="hover:bg-gray-50  cursor-pointer transition-colors"
                                            onClick={() => navigate('/calls', { state: { date: row.date, selectedEmployee } })}
                                        >
                                            <td className="px-6 py-3 font-medium text-gray-900 ">
                                                {format(parseISO(row.date), 'MMM d, yyyy')}
                                            </td>
                                            <td className="px-6 py-3 text-right ">{row.total_calls}</td>
                                            <td className="px-6 py-3 text-right font-mono text-xs ">{row.formatted_duration}</td>
                                            <td className="px-6 py-3 text-right text-green-600 ">{row.connected}</td>
                                            <td className="px-6 py-3 text-right text-red-600 ">{row.not_connected}</td>
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
            </div>
        </div>
    );
}
