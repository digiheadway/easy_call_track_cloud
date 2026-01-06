# 📦 LinkBox – Android App

**Secure Asset Sharing & Management Platform**

---

## 🧠 What is LinkBox ?

**LinkBox ** is an **Android application** that allows users to **store, manage, and securely share digital assets** such as:

- Links
- Files (rich text / markdown)
- Folders

All shared content is **opened inside the app**, not on the web.

(Simple example: Google Drive + Secure Link Sharing + Points System)

---

## 🔗 Universal Sharing Link Structure

All assets are shared using a **single universal link format**:

```
https://api.pokipro.com/linkbox/?token=4e03f880
```

### Important Behavior

- This URL **does NOT render any web page**
- This URL **does NOT fetch or show asset data**
- The only purpose of this link is to:
  - Open the **LinkBox Android App**
  - Pass the `token` value via Android Intent

(Security benefit: no data leakage, no scraping)

---

## 📱 Deep Linking & Token Flow

1. User clicks the shared link
2. Android checks for LinkBox app
3. If app exists:
   - App opens via Deep Link / App Link
   - Token is extracted from URL
4. App navigates to **Shared Content View Screen**
5. Backend validates:
   - Token exists
   - Link is active
   - Link is not expired
   - User has sufficient points

If token is invalid or expired → show error screen in app

---

## 🔐 First-Time Access Rule

- Asset must be opened **inside the app at least once**
- External access is blocked by default
- User can only:
  - Save copy
  - Extract link
  - Share externally

**If permissions are allowed by link creator**

---

## 📁 Asset Types

### 1. File
- Rich text viewer and editor
- Markdown powered
- Live updates if link is active

### 2. Folder
- Can contain files, links, subfolders
- Breadcrumb path supported
- Empty folder guidance shown

### 3. Link
- Normal website URL
- Always opens inside **in-app WebView**

---

## ⚙️ Asset Actions

Available actions on assets:

- Open
- Edit
- Rename
- Change location (move)
- Delete

(All actions open via bottom modal)

---

## 🔗 Asset Sharing System

Each asset can have **multiple sharing links**.

Each link can have its own:
- Name
- Description
- Expiry date
- Point cost
- Permissions

### Sharing Options per Link

- Sharing Access (link-based)
- Allow Save Copy
- External Sharing
  - Screenshot
  - View original URL
  - Share outside app
- Further Sharing
  - Allow receiver to reshare link

---

## 🧾 Create Link Flow

When creating a link:

- Auto-fill name = asset name
- Ask for:
  - Name
  - Description
  - Expiry
  - Cost (points)
  - Permissions

After creation:
- Rename
- Edit description
- Copy link
- Turn off access
- Delete link

### Live Sync Rule

If receiver has **not saved a copy**, any change in:
- Asset name
- Asset content
- Link permissions

Will reflect instantly

---

## 📊 My Links Section

### Summary Metrics

- Total Clicks
- Views
- Users
- New Users
- Points Earned

### Earnings Logic

- 0.1 point per view
- 5 points per new user
- 40% of link cost goes to creator

(Detailed breakdown shown in modal)

---

## 🔗 Link List View

Each link card shows:
- Name
- Views
- Users
- Active / Inactive status
- Point cost

Actions:
- Edit
- Delete
- Turn off access
- Expiry
- Further sharing toggle
- View asset
- Go to asset settings

Filter chips:
- Active
- Inactive

---

## 🕘 Link History

- Shows all accessed links
- Option to star important links
- Filter starred links

---

## 💰 Points System

### Earn Points By:
- Watching rewarded ads
- Sharing links
- New users via your link
- Buying points

### Spend Points On:
- Opening paid links
- Accessing premium links
- Subscription

### Insufficient Points Screen

Shows:
- Current balance
- Required points
- Options:
  - Watch ad
  - Earn Point
  - Buy points

---

## 🧭 App Navigation Tabs

1. **My Assets** – Manage files, folders, links
2. **My Links** – Shared links and earnings
3. **Link History** – Accessed links
4. **Profile**
   - Edit profile
   - Subscription
   - Appearance (Dark mode)
   - About
   - Privacy Policy
   - Terms
   - Logout

---

## 🎨 UI & UX Guidelines

- Bottom sheet modals
- Full width
- Height as per content
- Icon-driven UI
- Compact spacing
- Smooth fast animations
- Light gradients only
- Swipe to refresh
- Helpful empty states
- Tooltips for first-time users
- Exit confirmation on back press

---

## 📢 Ads System

Supported ads:
- Rewarded ads
- Interstitial ads
- Banner ads
- App open ads

### Ad Management

- Central config file
- Toggle ads ON/OFF
- Change placement IDs
- Subscription removes ads

---

## 🔙 Backend (Initial Phase)

- Firestore database
- Google Anonymous Authentication (testing)
- Token-based access control
- Secure rules for:
  - Asset access
  - Link permissions
  - Point balance

---

## 📌 Supporting Screens (Must Have)

- Login / Signup
- Empty folder screens
- Empty history screens
- Insufficient points screen
- Points balance always visible in header
- In-app WebView for links

---

## 🗄️ Firestore Database Structure

The app uses Firebase Firestore with the following collections:

### Collections Overview

```
📁 Firestore Database
├── 📂 users
├── 📂 assets
├── 📂 links
├── 📂 history
└── 📂 points_transactions
```

---

### 1. `users` Collection

**Document ID:** `{userId}` (Firebase Auth UID)

| Field | Type | Description |
|-------|------|-------------|
| `userId` | `string` | Firebase Auth UID (primary key) |
| `name` | `string` | Display username |
| `email` | `string` | User email (optional) |
| `points` | `number` | Current point balance |
| `accessToken` | `string?` | Optional access token |
| `totalEarned` | `number` | Lifetime points earned |

**Example Document:**
```json
{
  "userId": "abc123xyz",
  "name": "JohnDoe",
  "email": "john@example.com",
  "points": 150,
  "accessToken": null,
  "totalEarned": 500.0
}
```

---

### 2. `assets` Collection

**Document ID:** `{assetId}` (UUID)

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | Asset UUID (primary key) |
| `ownerId` | `string` | User ID who owns this asset |
| `type` | `string` | Asset type: `FILE`, `FOLDER`, `LINK` |
| `name` | `string` | Asset display name |
| `description` | `string` | Optional description |
| `content` | `string` | Actual content (markdown/url) |
| `parentId` | `string?` | Parent folder ID (null for root) |
| `allowSaveCopy` | `boolean` | Allow viewers to save a copy |
| `allowExternalSharing` | `boolean` | Allow sharing outside app |
| `allowFurtherSharing` | `boolean` | Allow re-sharing the link |
| `createdAt` | `number` | Unix timestamp (ms) |
| `updatedAt` | `number` | Unix timestamp (ms) |

**Example Document:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "ownerId": "abc123xyz",
  "type": "FILE",
  "name": "My Notes",
  "description": "Personal notes document",
  "content": "# Hello World\n\nThis is my content.",
  "parentId": null,
  "allowSaveCopy": true,
  "allowExternalSharing": true,
  "allowFurtherSharing": true,
  "createdAt": 1703721600000,
  "updatedAt": 1703808000000
}
```

---

### 3. `links` Collection

**Document ID:** `{token}` (8-char unique token)

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | Link UUID |
| `assetId` | `string` | Reference to asset being shared |
| `token` | `string` | Unique share token (primary key) |
| `name` | `string` | Custom link name |
| `pointCost` | `number` | Points required to access |
| `expiryDate` | `number?` | Expiry timestamp (null = never) |
| `status` | `string` | `ACTIVE`, `INACTIVE`, `EXPIRED` |
| `newUsers` | `number` | New app installs via link |
| `users` | `number` | Unique users who accessed |
| `views` | `number` | Total view count |
| `createdAt` | `number` | Unix timestamp (ms) |
| `updatedAt` | `number` | Unix timestamp (ms) |

> **Note:** Owner details and description are fetched via `assetId` → `assets.ownerId` → `users`

**Example Document:**
```json
{
  "id": "link-uuid-here",
  "assetId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "token": "4e03f880",
  "name": "My Shared Notes",
  "pointCost": 5,
  "expiryDate": null,
  "status": "ACTIVE",
  "newUsers": 3,
  "users": 15,
  "views": 42,
  "createdAt": 1703721600000,
  "updatedAt": 1703808000000
}
```

---

### 4. `history` Collection

**Document ID:** `{userId}_{token}`

| Field | Type | Description |
|-------|------|-------------|
| `token` | `string` | Reference to sharing link token |
| `userId` | `string` | User who accessed the link |
| `accessedAt` | `number` | Unix timestamp (ms) |
| `isStarred` | `boolean` | User starred this item |

> **Note:** Asset details (name, type, owner, description, pointCost) are fetched dynamically via `token` → `links` → `assets` → `users`

**Example Document:**
```json
{
  "token": "4e03f880",
  "userId": "user456xyz",
  "accessedAt": 1703894400000,
  "isStarred": true
}
```

---

### 5. `points_transactions` Collection

**Document ID:** `{transactionId}` (UUID)

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | Transaction UUID (primary key) |
| `userId` | `string` | User involved in transaction |
| `points` | `number` | Points amount (+/-) |
| `type` | `string` | `EARN`, `SPEND`, `BONUS`, `PURCHASE` |
| `remark` | `string` | Transaction description |
| `createdAt` | `number` | Unix timestamp (ms) |
| `updatedAt` | `number` | Unix timestamp (ms) |

**Example Document:**
```json
{
  "id": "txn-uuid-here",
  "userId": "abc123xyz",
  "points": 5,
  "type": "EARN",
  "remark": "Earned from link views (token: 4e03f880)",
  "createdAt": 1703894400000,
  "updatedAt": 1703894400000
}
```

---

### 📊 Data Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         users                                    │
│  userId (PK) | name | points | accessToken | totalEarned        │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 1:N
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         assets                                   │
│  id (PK) | ownerId (FK→users) | type | name | content | parent  │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 1:N
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                          links                                   │
│  token (PK) | assetId (FK→assets) | ownerId | permissions...    │    │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 1:N
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         history                                  │
│  userId_token (PK) | token (FK→links) | userId (FK→users)      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    points_transactions                           │
│  id (PK) | userId (FK→users) | points | type | remark           │
└─────────────────────────────────────────────────────────────────┘
```

---

### 🔒 Firestore Security Rules (Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users: Only authenticated user can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Assets: Only owner can manage their assets
    match /assets/{assetId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                   request.resource.data.ownerId == request.auth.uid;
    }
    
    // Links: Anyone authenticated can read, only owner can write
    match /links/{token} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                   request.resource.data.ownerId == request.auth.uid;
    }
    
    // History: User can only access their own history
    match /history/{historyId} {
      allow read, write: if request.auth != null && 
                         historyId.matches(request.auth.uid + '_.*');
    }
    
    // Points Transactions: User can only access their own transactions
    match /points_transactions/{transactionId} {
      allow read: if request.auth != null && 
                  resource.data.userId == request.auth.uid;
      allow write: if request.auth != null && 
                   request.resource.data.userId == request.auth.uid;
    }
  }
}
```

---

## ✅ Final Notes

- No asset data is ever exposed on web
- All access is token + app controlled
- App-first security model
- Scalable for future web or iOS support

---

**End of README**

