# Call History Manager

A rich Android application for managing and viewing call history with advanced filtering and grouping capabilities.

## Features

### Core Features
- **Call History Display**: View all call history in a clean, organized interface
- **Grouping by Number**: Calls are automatically grouped by phone number for better organization
- **Advanced Filtering**: Filter calls by type (incoming, outgoing, missed), date range, and search queries
- **Statistics Dashboard**: View comprehensive call statistics including total calls, duration, and contact counts

### Key Components

#### Data Models
- `CallHistoryItem`: Represents individual call records
- `CallGroup`: Groups calls by phone number with aggregated data
- `CallFilter`: Handles filtering and search functionality

#### Repository Layer
- `CallHistoryRepository`: Manages data access and business logic
- Fetches call history from Android's CallLog provider
- Implements filtering and grouping algorithms

#### UI Components
- **CallGroupCard**: Displays grouped call information
- **CallHistoryItemCard**: Shows individual call details
- **StatisticsCard**: Presents call statistics in an attractive format
- **FilterDialog**: Advanced filtering interface

#### Screens
- **Main Screen**: Lists all call groups with statistics
- **Detail Screen**: Shows individual calls within a group

## Permissions Required

The app requires the following permissions:
- `READ_CALL_LOG`: Access to call history
- `READ_PHONE_STATE`: Phone state information
- `READ_CONTACTS`: Contact information for better display

## Architecture

The app follows MVVM architecture with:
- **View**: Jetpack Compose UI components
- **ViewModel**: Manages UI state and business logic
- **Repository**: Handles data access and processing
- **Data Models**: Clean data structures for call information

## Features in Detail

### Filtering System
- Filter by call type (incoming, outgoing, missed)
- Date range filtering
- Search by contact name or phone number
- Duration-based filtering

### Grouping System
- Automatic grouping by phone number
- Shows total calls per contact
- Displays last call date and total duration
- Maintains chronological order within groups

### Statistics
- Total call count
- Breakdown by call type
- Unique contact count
- Total call duration

## Usage

1. **Launch the app**: The app will request necessary permissions
2. **View call history**: All calls are automatically grouped by phone number
3. **Use filters**: Tap the filter icon to apply advanced filtering
4. **View statistics**: Toggle statistics visibility with the eye icon
5. **Explore details**: Tap on any group to see individual calls
6. **Refresh data**: Use the refresh button to reload call history

## Technical Implementation

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Data Source**: Android CallLog provider
- **State Management**: StateFlow and Compose state
- **Navigation**: Simple state-based navigation

## Future Enhancements

Potential improvements could include:
- Export functionality
- Call analytics and insights
- Backup and restore
- Advanced search capabilities
- Call categorization
- Integration with calendar events
