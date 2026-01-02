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
    TrendingUp
} from 'lucide-react';

export default function ReportsPage() {
    const [loading, setLoading] = useState(true);
    const [reportData, setReportData] = useState({
        summary: {},
        by_employee: [],
        by_date: []
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

    return (
        <div className="relative">
            {/* Floating Loading Indicator */}
            {loading && (
                <div className="absolute inset-x-0 top-12 z-10 flex justify-center pointer-events-none">
                    <div className="bg-white shadow-md border px-4 py-2 rounded-full flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                        <span className="text-sm font-medium text-gray-600">Updating...</span>
                    </div>
                </div>
            )}

            <div className={`space-y-8 ${loading ? 'opacity-60 transition-opacity duration-200' : ''}`}>
                <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight">Reports</h1>
                        <p className="text-gray-500 text-sm mt-1">Detailed analytics and performance metrics.</p>
                    </div>

                    <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 w-full lg:w-auto">
                        {/* Date Range Chips */}
                        <DateRangeFilter
                            value={dateRange}
                            onChange={setDateRange}
                            customRange={customRange}
                            onCustomRangeChange={setCustomRange}
                        />

                        {/* Employee Dropdown */}
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

                {/* Employee Performance Table */}
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold flex items-center gap-2">
                            <TrendingUp size={20} className="text-blue-600" />
                            Employee Performance
                        </h2>
                        <button className="btn btn-ghost text-sm gap-2 text-gray-600">
                            <Download size={16} />
                            Export CSV
                        </button>
                    </div>

                    <div className="card !p-0 overflow-hidden border border-gray-200">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 font-medium">
                                    <tr>
                                        <th className="px-6 py-4">Employee</th>
                                        <th className="px-6 py-4 text-right">Total Calls</th>
                                        <th className="px-6 py-4 text-right">Inbound</th>
                                        <th className="px-6 py-4 text-right">Outbound</th>
                                        <th className="px-6 py-4 text-right">Missed</th>
                                        <th className="px-6 py-4 text-right">Total Duration</th>
                                        <th className="px-6 py-4 text-right">Avg Duration</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {reportData.by_employee?.map((emp, idx) => (
                                        <tr key={emp.employee_id || idx} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4 font-medium text-gray-900">{emp.name}</td>
                                            <td className="px-6 py-4 text-right font-semibold">{emp.total_calls}</td>
                                            <td className="px-6 py-4 text-right text-blue-600">{emp.inbound}</td>
                                            <td className="px-6 py-4 text-right text-green-600">{emp.outbound}</td>
                                            <td className="px-6 py-4 text-right text-red-500">{emp.missed}</td>
                                            <td className="px-6 py-4 text-right font-mono text-xs">{emp.formatted_duration}</td>
                                            <td className="px-6 py-4 text-right font-mono text-xs text-gray-500">{emp.avg_duration || '-'}</td>
                                        </tr>
                                    ))}
                                    {!loading && (!reportData.by_employee || reportData.by_employee.length === 0) && (
                                        <tr>
                                            <td colSpan="7" className="p-8 text-center text-gray-500">
                                                No performance data available for this period
                                            </td>
                                        </tr>
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
