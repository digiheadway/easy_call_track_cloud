# Quick DateTime Input Feature

## Overview

The QuickDateTimeInput component provides a user-friendly way to select dates and times for task management with predefined quick options and custom input fallbacks.

## Features

### Quick Date Options
- **Today** - Current date
- **T+1** - Tomorrow (Default selection)
- **T+2** - Day after tomorrow
- **T+5** - 5 days from today
- **T+7** - 1 week from today
- **T+15** - 2 weeks from today
- **T+30** - 1 month from today
- **T+60** - 2 months from today
- **Custom** - Opens date picker

### Quick Time Options
- **11 AM** - 11:00 AM (Default selection)
- **2 PM** - 2:00 PM
- **5 PM** - 5:00 PM
- **7 PM** - 7:00 PM
- **Custom** - Opens time picker

## Usage

### Basic Implementation

```tsx
import QuickDateTimeInput from '../common/QuickDateTimeInput';

// Date input
<QuickDateTimeInput
  type="date"
  label="Date *"
  value={dateValue}
  onChange={handleDateChange}
  required
/>

// Time input
<QuickDateTimeInput
  type="time"
  label="Time *"
  value={timeValue}
  onChange={handleTimeChange}
  required
/>
```

### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `type` | `'date' \| 'time'` | Yes | Specifies whether this is a date or time input |
| `label` | `string` | Yes | Label displayed above the input |
| `value` | `string` | Yes | Current value (ISO date string for date, HH:mm for time) |
| `onChange` | `(value: string) => void` | Yes | Callback when value changes |
| `required` | `boolean` | No | Whether the input is required (default: false) |
| `min` | `string` | No | Minimum value (useful for date inputs) |

## Implementation Details

### Where It's Used

The QuickDateTimeInput component is now integrated into:

1. **AddTodoModal** - For creating new tasks
2. **TodoActionModal** - For rescheduling tasks
3. **AddActivityModal** - For creating new activities
4. **TodoViewModal** - For editing existing tasks

### How It Works

1. **Default Values** - Automatically sets T+1 for date and 11 AM for time when no value is provided
2. **Quick Selection** - Users can click on predefined date/time options for instant selection
3. **Visual Feedback** - Selected option is highlighted with blue styling
4. **Custom Input** - If "Custom" is selected, a standard HTML input appears
5. **Smart Detection** - Automatically detects if a custom value matches any predefined option
6. **Value Display** - Selected values are displayed in a user-friendly format
7. **Change Option** - Users can click "Change" to modify the selection

### Date Calculations

The component automatically calculates relative dates based on the current date:
- T+1 adds 1 day to today (default)
- T+7 adds 7 days to today
- T+30 adds 30 days to today
- etc.

### Time Formatting

Times are displayed in 12-hour format (e.g., "2:00 PM") but stored in 24-hour format internally (e.g., "14:00").

### Smart Option Detection

The component automatically detects which predefined option matches the current value:
- For dates: Calculates the difference in days and matches to the appropriate T+X option
- For times: Checks if the time matches any of the predefined time slots
- If no match is found, automatically selects "Custom"

## Styling

The component uses Tailwind CSS classes and includes:
- **Selected State** - Blue highlighting for the currently selected option
- **Hover Effects** - Interactive feedback on buttons
- **Focus States** - Blue rings for accessibility
- **Responsive Grid Layout** - Adapts to different screen sizes
- **Consistent Spacing** - Uniform typography and spacing
- **Smooth Transitions** - Animated state changes

## Benefits

1. **Faster Task Creation** - Users can quickly select common dates/times
2. **Better UX** - No need to manually type or navigate date pickers for common selections
3. **Consistency** - Standardized date/time selection across all task-related forms
4. **Flexibility** - Still allows custom input when needed
5. **Mobile Friendly** - Touch-friendly button sizes and responsive layout
6. **Smart Defaults** - Automatically sets sensible defaults (T+1, 11 AM)
7. **Visual Clarity** - Clear indication of which option is currently selected

## Recent Fixes

- **Default Values** - Now automatically sets T+1 for date and 11 AM for time
- **Selection State** - Properly tracks and displays which option is selected
- **Value Updates** - Fixed issues with values not updating correctly
- **Custom Input** - Improved handling of custom date/time input
- **Smart Detection** - Automatically detects when custom values match predefined options

## Future Enhancements

Potential improvements could include:
- User-configurable quick options
- Recent selections history
- Calendar view for date selection
- Time zone support
- Integration with user preferences
- Keyboard shortcuts for quick selection
