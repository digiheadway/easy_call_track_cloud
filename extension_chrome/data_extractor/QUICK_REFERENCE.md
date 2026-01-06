# Data Scanner - Quick Reference

## 🎯 At a Glance

**Extension Name**: Data Scanner  
**Purpose**: Extract and manage phone numbers and emails from web pages  
**Width**: 500px | **Height**: ~400px

---

## 📋 Interface Layout

```
┌──────────────────────────────────────────────┐
│  Data Scanner                                 │
├──────────────────────────────────────────────┤
│  ☑ Detect Phone    ☑ Detect Email           │
│  [Start Scanner]                              │
├──────────────────────────────────────────────┤
│  Phones (5)  │  Emails (3)                   │
├──────────────────────────────────────────────┤
│  ID │ Value │ Time │ URL │ Context │ [Copy] │
│   5 │ +1... │ 2m   │ ... │ ...     │ [Copy] │
│   4 │ +1... │ 5h   │ ... │ ...     │ [Copy] │
│   3 │ +1... │ 1d   │ ... │ ...     │ [Copy] │
├──────────────────────────────────────────────┤
│  [Download Phones]  [Clear Results]          │
└──────────────────────────────────────────────┘
```

---

## ⚙️ Settings

| Setting | Default | Purpose |
|---------|---------|---------|
| Detect Phone | ✅ Checked | Scan for phone numbers |
| Detect Email | ✅ Checked | Scan for email addresses |

**Note**: Settings persist across sessions

---

## 🎛️ Controls

| Button | Action | Location |
|--------|--------|----------|
| Start Scanner | Begin extraction | Top |
| Stop Scanner | Pause extraction | Top (when active) |
| Copy | Copy value to clipboard | Each row |
| Download Phones | Export phones to CSV | Phones tab |
| Download Emails | Export emails to CSV | Emails tab |
| Clear Results | Remove tab data | Each tab |

---

## 📊 Table Columns

| Column | Content | Width | Truncated |
|--------|---------|-------|-----------|
| ID | Sequential number | 40px | No |
| Phone/Email | Actual value | Auto | No |
| Time | Relative timestamp | 80px | No |
| URL | Source page | 120px | Yes* |
| Context | Nearby text | 150px | Yes* |
| Actions | Copy button | 60px | No |

*Hover for full text in tooltip

---

## ⏰ Time Format

| Display | Meaning |
|---------|---------|
| Just now | < 1 minute |
| 5m ago | 5 minutes ago |
| 3h ago | 3 hours ago |
| 11/29/2025 | Full date |

---

## 📥 CSV Export

**Filename**: `phones_[timestamp].csv` or `emails_[timestamp].csv`

**Format**:
```csv
ID,Value,Timestamp,URL,Context
1,"+1-555-0123","2025-11-29T00:21:58.123Z","https://example.com","Contact:"
```

---

## 🎨 Color Coding

| Element | Color | Hex |
|---------|-------|-----|
| Primary (buttons, tabs) | Blue | #2563eb |
| Text | Dark Gray | #1e293b |
| Secondary | Gray | #64748b |
| Danger (clear) | Red | #ef4444 |
| Success (copied) | Green | #10b981 |

---

## ⌨️ Keyboard Shortcuts

None currently implemented.

---

## 🔄 Data Flow

```
User clicks "Start Scanner"
        ↓
Content script scans page
        ↓
Detects emails/phones (based on settings)
        ↓
Extracts context & metadata
        ↓
Saves to chrome.storage.local
        ↓
Updates popup UI
        ↓
User views in tabs
        ↓
User copies or downloads
```

---

## 📱 Bottom-Right Indicator

When scanning is active, a small panel appears on the page:

```
┌──────────────────┐
│ 🔍 Detecting...  │
│ Collected: 12    │
└──────────────────┘
```

Shows checkmark (✓) when new item detected.

---

## 🎯 Best Practices

1. **Enable only what you need** - Uncheck unused detection types
2. **Download regularly** - Export data before clearing
3. **Check context** - Hover over truncated text for full info
4. **Use copy button** - Faster than manual selection
5. **Clear per type** - Remove phones or emails separately

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| No data appearing | Reload page after starting scanner |
| Copy not working | Check browser clipboard permissions |
| Settings not saving | Check chrome.storage quota |
| Table not scrolling | Ensure popup is fully loaded |

---

## 📊 Storage

- **Location**: `chrome.storage.local`
- **Keys**: 
  - `collectedData` - Array of items
  - `detectPhone` - Boolean
  - `detectEmail` - Boolean
  - `extractionActive` - Boolean

---

## 🔒 Privacy

- ✅ All data stored locally
- ✅ No external servers
- ✅ No tracking
- ✅ Manual control

---

## 📈 Limits

- **Storage**: Chrome's storage quota (~5MB)
- **Table rows**: No hard limit (scrollable)
- **CSV export**: No limit
- **Detection**: Unlimited

---

## 🎨 UI States

| State | Visual |
|-------|--------|
| Tab active | Blue underline + blue text |
| Tab inactive | Gray text |
| Button hover | Lighter background |
| Copy success | Green background + ✓ |
| Empty table | "No data collected yet" |

---

## 🚀 Quick Start

1. Click extension icon
2. Ensure checkboxes are set
3. Click "Start Scanner"
4. Browse web pages
5. Click icon again to view data
6. Click "Copy" to copy items
7. Click "Download" to export
8. Click "Clear" to reset

---

**Version**: 1.0  
**Last Updated**: 2025-11-29
