# Implementation Plan: Enhanced List Views with Icons and Unified Design

## Objective
Refine the Pipeline and Contact list screens to provide:
1. Auto-focus on search field when search icon is clicked
2. Three view modes: Card, List, and Table
3. Icon-based UI for labels, priority, stage, notes, and other fields
4. Unified component design between Pipeline and Contact screens

## Changes Required

### 1. ListScreens.kt
- [x] Add `ViewMode` enum (CARD, LIST, TABLE)
- [x] Add `FocusRequester` for search field
- [ ] Fix search icon click handler to properly request focus
- [ ] Update view mode menu to cycle through CARD → LIST → TABLE
- [ ] Add `focusRequester` modifier to search field
- [ ] Apply same changes to ContactsScreen
- [ ] Update PersonList component calls to use viewMode instead of isListView

### 2. Components.kt  
- [ ] Create unified `PersonCard` component that works for both Pipeline and Contacts
- [ ] Add icon-based badges for:
  - Priority (flag icon with color)
  - Stage (status icon with color)
  - Labels (tag icon)
  - Notes (note icon)
  - Source (source-specific icons)
- [ ] Create `PersonTableRow` component for table view
- [ ] Update `PersonList` to support TABLE view mode
- [ ] Ensure visual consistency between Pipeline and Contact cards

### 3. Icon Mapping
- Priority: `Icons.Default.Flag` with priority color
- Stage: `Icons.Default.Circle` or `Icons.Default.FiberManualRecord` with stage color
- Labels: `Icons.Default.Label` or `Icons.Default.LocalOffer`
- Notes: `Icons.Default.Note` or `Icons.Default.Description`
- Source: Map each source to appropriate icon
- Phone: `Icons.Default.Phone`
- Budget: `Icons.Default.AttachMoney` or `Icons.Default.CurrencyRupee`
- Follow-up: `Icons.Default.Event` or `Icons.Default.Schedule`

## Implementation Steps

1. Fix the malformed search click handler in ListScreens.kt
2. Update view mode dropdown to support all three modes
3. Add focusRequester to CompactSearchField
4. Create unified PersonCard component
5. Add icon-based visual elements
6. Create PersonTableRow for table view
7. Update PersonList to handle all three view modes
8. Apply same pattern to ContactsScreen
9. Test all view modes and ensure consistency

## Status
- In Progress: Fixing search focus and view mode implementation
