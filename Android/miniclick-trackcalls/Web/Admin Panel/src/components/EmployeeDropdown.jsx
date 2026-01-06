import { useState, useEffect, useRef } from 'react';
import { ChevronDown, Users, Check } from 'lucide-react';
import api from '../api/client';

export default function EmployeeDropdown({
    value,
    onChange,
    employees: externalEmployees = null,  // Can be passed in or fetched
    className = '',
}) {
    const [employees, setEmployees] = useState(externalEmployees || []);
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef(null);

    // Fetch employees if not provided externally
    useEffect(() => {
        if (!externalEmployees) {
            fetchEmployees();
        }
    }, [externalEmployees]);

    // Update internal state when external employees change
    useEffect(() => {
        if (externalEmployees) {
            setEmployees(externalEmployees);
        }
    }, [externalEmployees]);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const fetchEmployees = async () => {
        setLoading(true);
        try {
            const res = await api.get('/employees.php');
            if (res.data && Array.isArray(res.data)) {
                setEmployees(res.data);
            }
        } catch (err) {
            console.error("Failed to fetch employees", err);
        } finally {
            setLoading(false);
        }
    };

    const selectedEmployee = employees.find(emp => String(emp.id) === String(value));
    const displayText = selectedEmployee ? selectedEmployee.name : 'All Employees';

    const handleSelect = (employeeId) => {
        onChange(employeeId);
        setIsOpen(false);
    };

    return (
        <div className={`relative ${className}`} ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="h-[38px] min-w-[180px] flex items-center justify-between gap-2 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:ring-2 focus:ring-blue-500 outline-none transition-colors"
            >
                <div className="flex items-center gap-2">
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium ${selectedEmployee
                        ? 'bg-blue-100 dark:bg-blue-600/10 text-blue-600 dark:text-blue-400'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
                        }`}>
                        {selectedEmployee ? (
                            selectedEmployee.name?.substring(0, 2).toUpperCase()
                        ) : (
                            <Users size={14} />
                        )}
                    </div>
                    <span className="font-medium text-gray-700 dark:text-gray-200 truncate max-w-[120px]">
                        {displayText}
                    </span>
                </div>
                <ChevronDown
                    size={16}
                    className={`text-gray-400 transition-transform flex-shrink-0 ${isOpen ? 'rotate-180' : ''}`}
                />
            </button>

            {/* Dropdown Menu */}
            {isOpen && (
                <div className="absolute right-0 top-full mt-1 w-full min-w-[200px] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-50 py-1 animate-in fade-in slide-in-from-top-2 duration-150">
                    {/* All Employees Option */}
                    <button
                        onClick={() => handleSelect('')}
                        className={`w-full flex items-center justify-between gap-2 px-3 py-2.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors ${!value ? 'bg-blue-50 dark:bg-blue-600/10 text-blue-700 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300'
                            }`}
                    >
                        <div className="flex items-center gap-2">
                            <div className="w-6 h-6 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 flex items-center justify-center">
                                <Users size={14} />
                            </div>
                            <span className="font-medium">All Employees</span>
                        </div>
                        {!value && <Check size={16} className="text-blue-600" />}
                    </button>

                    {employees.length > 0 && (
                        <div className="border-t border-gray-100 dark:border-gray-700 my-1" />
                    )}

                    {/* Employee List */}
                    {loading ? (
                        <div className="px-3 py-4 text-center text-gray-500 dark:text-gray-400 text-sm">
                            Loading...
                        </div>
                    ) : (
                        <div className="max-h-[250px] overflow-y-auto">
                            {employees.map((emp) => {
                                const isSelected = String(emp.id) === String(value);
                                return (
                                    <button
                                        key={emp.id}
                                        onClick={() => handleSelect(emp.id)}
                                        className={`w-full flex items-center justify-between gap-2 px-3 py-2.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors ${isSelected ? 'bg-blue-50 dark:bg-blue-600/10 text-blue-700 dark:text-blue-400' : 'text-gray-700 dark:text-gray-300'
                                            }`}
                                    >
                                        <div className="flex items-center gap-2">
                                            <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${isSelected
                                                ? 'bg-blue-100 dark:bg-blue-600/10 text-blue-600 dark:text-blue-400'
                                                : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                                                }`}>
                                                {emp.name?.substring(0, 2).toUpperCase()}
                                            </div>
                                            <span className="font-medium truncate">{emp.name}</span>
                                        </div>
                                        {isSelected && <Check size={16} className="text-blue-600 flex-shrink-0" />}
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
