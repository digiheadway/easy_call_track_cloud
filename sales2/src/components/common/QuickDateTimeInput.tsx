import React, { useState, useEffect, useCallback } from 'react';

interface QuickDateTimeInputProps {
  value: string;
  onChange: (value: string) => void;
  type: 'date' | 'time';
  label: string;
  required?: boolean;
  min?: string;
}

const QuickDateTimeInput: React.FC<QuickDateTimeInputProps> = ({
  value,
  onChange,
  type,
  label,
  required = false,
  min
}) => {
  const [showCustom, setShowCustom] = useState(false);
  const [selectedOption, setSelectedOption] = useState<string>('');
  const [isInitialized, setIsInitialized] = useState(false);

  const quickDateOptions = [
    { label: 'Today', value: 'today' },
    { label: 'T+1', value: 't+1' },
    { label: 'T+2', value: 't+2' },
    { label: 'T+5', value: 't+5' },
    { label: 'T+7', value: 't+7' },
    { label: 'T+15', value: 't+15' },
    { label: 'T+30', value: 't+30' },
    { label: 'T+60', value: 't+60' },
    { label: 'Custom', value: 'custom' }
  ];

  const quickTimeOptions = [
    { label: '11 AM', value: '11:00' },
    { label: '2 PM', value: '14:00' },
    { label: '5 PM', value: '17:00' },
    { label: '7 PM', value: '19:00' },
    { label: 'Custom', value: 'custom' }
  ];

  const options = type === 'date' ? quickDateOptions : quickTimeOptions;

  const findMatchingOption = useCallback((currentValue: string): string => {
    if (type === 'date') {
      const today = new Date();
      // Reset time to start of day to avoid timezone issues
      today.setHours(0, 0, 0, 0);
      
      const currentDate = new Date(currentValue);
      // Reset time to start of day for comparison
      currentDate.setHours(0, 0, 0, 0);
      
      const diffTime = currentDate.getTime() - today.getTime();
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));
      

      
      if (diffDays === 0) return 'today';
      if (diffDays === 1) return 't+1';
      if (diffDays === 2) return 't+2';
      if (diffDays === 5) return 't+5';
      if (diffDays === 7) return 't+7';
      if (diffDays === 15) return 't+15';
      if (diffDays === 30) return 't+30';
      if (diffDays === 60) return 't+60';
      return 'custom';
    } else {
      // For time, check if it matches any predefined time
      const timeOptions = ['11:00', '14:00', '17:00', '19:00'];
      if (timeOptions.includes(currentValue)) {
        return currentValue;
      }
      return 'custom';
    }
  }, [type]);

  // Set default values when component mounts
  useEffect(() => {
    if (!isInitialized) {
      if (!value) {
        if (type === 'date') {
          // Default to T+1 (tomorrow)
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          const tomorrow = new Date(today);
          tomorrow.setDate(today.getDate() + 1);
          
          const year = tomorrow.getFullYear();
          const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
          const day = String(tomorrow.getDate()).padStart(2, '0');
          const dateString = `${year}-${month}-${day}`;
          
          onChange(dateString);
          setSelectedOption('t+1');
        } else {
          // Default to 11 AM
          onChange('11:00');
          setSelectedOption('11:00');
        }
      } else {
        // Determine which option matches the current value
        const matchingOption = findMatchingOption(value);
        setSelectedOption(matchingOption);
      }
      setIsInitialized(true);
    } else if (value) {
      // Update selected option when value changes
      const matchingOption = findMatchingOption(value);
      setSelectedOption(matchingOption);
      if (matchingOption !== 'custom') {
        setShowCustom(false);
      }
    }
  }, [isInitialized, value, type, onChange, findMatchingOption]);

  const handleQuickSelect = (optionValue: string) => {
    if (optionValue === 'custom') {
      setShowCustom(true);
      setSelectedOption('custom');
      return;
    }

    setSelectedOption(optionValue);
    setShowCustom(false);

    if (type === 'date') {
      const today = new Date();
      // Reset time to start of day to avoid timezone issues
      today.setHours(0, 0, 0, 0);
      
      let targetDate = new Date(today);

      switch (optionValue) {
        case 'today':
          // Keep today's date
          break;
        case 't+1':
          targetDate.setDate(today.getDate() + 1);
          break;
        case 't+2':
          targetDate.setDate(today.getDate() + 2);
          break;
        case 't+5':
          targetDate.setDate(today.getDate() + 5);
          break;
        case 't+7':
          targetDate.setDate(today.getDate() + 7);
          break;
        case 't+15':
          targetDate.setDate(today.getDate() + 15);
          break;
        case 't+30':
          targetDate.setDate(today.getDate() + 30);
          break;
        case 't+60':
          targetDate.setDate(today.getDate() + 60);
          break;
      }

      // Create date string in local timezone to avoid timezone conversion
      const year = targetDate.getFullYear();
      const month = String(targetDate.getMonth() + 1).padStart(2, '0');
      const day = String(targetDate.getDate()).padStart(2, '0');
      const dateString = `${year}-${month}-${day}`;
      
      onChange(dateString);
    } else {
      // For time, the optionValue is already in HH:mm format
      onChange(optionValue);
    }
  };

  const handleCustomInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    onChange(newValue);
    
    // Check if the new value matches any predefined option
    const matchingOption = findMatchingOption(newValue);
    if (matchingOption !== 'custom') {
      setSelectedOption(matchingOption);
      setShowCustom(false);
    }
  };

  const handleCustomInputBlur = () => {
    // Don't auto-hide custom input, let user control it
  };

  const formatDisplayValue = (val: string) => {
    if (!val) return '';
    
    if (type === 'date') {
      try {
        return new Date(val).toLocaleDateString('en-US', {
          weekday: 'short',
          year: 'numeric',
          month: 'short',
          day: 'numeric'
        });
      } catch {
        return val;
      }
    } else {
      // Format time to be more readable
      const [hours, minutes] = val.split(':');
      const hour = parseInt(hours);
      const ampm = hour >= 12 ? 'PM' : 'AM';
      const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
      return `${displayHour}:${minutes} ${ampm}`;
    }
  };

  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-2">{label}</label>
      
      {/* Quick Selection Buttons */}
      <div className="flex gap-2 mb-3 overflow-x-auto scrollbar-hide pb-1">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => handleQuickSelect(option.value)}
            className={`px-3 py-2 text-xs font-medium rounded-md transition-colors whitespace-nowrap flex-shrink-0 ${
              selectedOption === option.value
                ? 'bg-blue-100 text-blue-800 border-2 border-blue-500'
                : option.value === 'custom'
                ? 'bg-blue-50 text-blue-700 hover:bg-blue-100 border-2 border-blue-300 hover:border-blue-400'
                : 'bg-white text-gray-700 hover:bg-gray-50 border-2 border-gray-300 hover:border-gray-400'
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>

      {/* Custom Input */}
      {showCustom && (
        <div className="mb-3">
          <input
            type={type}
            value={value}
            onChange={handleCustomInputChange}
            onBlur={handleCustomInputBlur}
            className="input w-full focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            required={required}
            min={min}
            autoFocus
          />
        </div>
      )}

      {/* Current Value Display */}
      {!showCustom && value && (
        <div className="text-sm text-gray-600 bg-gray-50 px-3 py-2 rounded-md border border-gray-200 flex items-center justify-between">
          <span className="font-medium">
            {formatDisplayValue(value)}
          </span>
          <button
            type="button"
            onClick={() => {
              setShowCustom(true);
              setSelectedOption('custom');
            }}
            className="text-blue-600 hover:text-blue-800 underline text-xs hover:no-underline hover:bg-blue-50 px-2 py-1 rounded transition-colors"
          >
            Change
          </button>
        </div>
      )}
    </div>
  );
};

export default QuickDateTimeInput;