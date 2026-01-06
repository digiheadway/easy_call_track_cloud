# Product Requirements Document (PRD): LinkBox

## 1. Product Vision & Mission
**Vision**: To be the ultimate ecosystem for digital asset organization and value-driven sharing.
**Mission**: To empower users to curate, secure, and monetize their digital footprints through an intuitive, premium mobile experience.

---

## 2. Executive Summary
LinkBox is a multi-functional Android application designed for advanced link management, file organization, and social content discovery. It bridges the gap between simple bookmarking and complex file storage by introducing a **Point-Based Economy**, allowing users to earn and spend points to access exclusive shared content.

---

## 3. Core Features & Functional Requirements

### 3.1 Link & File Management ("My" System)
- **Hierarchical Storage**: Create folders and sub-folders to organize links and files.
- **Dynamic Page Editor**: A built-in editor to create custom landing pages with rich text, links, and media.
- **Asset Types**: Support for Links, Folders, and custom-designed Pages.
- **Operations**: Rename, Move, Delete, Star (Favorite), and Archive assets.

### 3.2 Premium Sharing System
- **Tokenized Links**: Generate secure, unique URLs (`https://api.pokipro.com/link.php?token=...`).
- **Access Control**:
    - Set point requirements for joining/accessing a link.
    - Privacy settings: Public vs. Private.
    - Expiry controls (optional).
- **Ownership**: Owners can bypass their own point requirements.

### 3.3 Points Economy & Monetization
- **Earn**: System for users to earn points through engagement, daily tasks, or content contribution.
- **Points Shop**: A marketplace to redeem points or acquire new features.
- **Link Economics**: Deduct points from visitors and reward creators.

### 3.4 Discovery & Social
- **Trending**: A curated feed of popular news and links from the community.
- **Joined Links**: A central repository for all links a user has successfully "unlocked" or joined.
- **User Profiles**: Manage personal identity, point balances, and cloud sync status.

---

## 4. User Experience (UX) Design

### 4.1 Design Principles
- **Premium Aesthetics**: Use of glassmorphism, subtle gradients, and high-quality iconography.
- **Fluid Motion**: Implementation of standard easing (e.g., `CubicBezierEasing`) for all transitions to ensure a "buttery" feel.
- **Mobile First**: All destructive or complex actions are mediated through **Bottom Sheets** (LinkBoxBottomSheet) rather than centered dialogs.

### 4.2 Key User Flows
1.  **Creation Flow**: `Add Button` -> `Choose Type (Link/Folder/Page)` -> `Input Details` -> `Save to Local/Cloud`.
2.  **Sharing Flow**: `Item Context Menu` -> `Share` -> `Set Points` -> `Generate URL` -> `Copy to Clipboard`.
3.  **Joining Flow**: `Deep Link Click` -> `DeepLinkScreen` -> `Verify Requirements` -> `Deduct Points` -> `Access Content`.

---

## 5. UI & Screen Specifications

| Screen | Description | Key Components |
| :--- | :--- | :--- |
| **My Screen** | The user's personal vault. | Tabbed view (Files/Folders), Search bar, FAB for creation. |
| **Earn Screen** | Gamified point earning hub. | Progression bars, Task lists, Point balance display. |
| **Joined Screen** | Repository of unlocked content. | List of joined links with creator info and access dates. |
| **Trending Screen** | Social discovery hub. | News cards, Image thumbnails, Quick-join buttons. |
| **DeepLink Screen** | The gateway for tokens. | "Unlock" button, Point requirement info, Preview cards. |
| **Page Editor** | Custom content creator. | Rich text inputs, link embedding, preview toggle. |
| **Settings** | Configuration and Account. | Theme toggle, Cloud sync status, Logout. |

---

## 6. Technical Architecture

### 6.1 Frontend (Android)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Concurrency**: Kotlin Coroutines & Flow
- **Navigation**: Compose Navigation with Custom Transitions (Sliding & Fading)
- **Premium Components**:
    - `LinkBoxBottomSheet`: Standardized interactive layer for all actions.
    - `Gradients & Easing`: Custom `CubicBezierEasing` for app-wide smoothness.

### 6.2 Data Layer & Sync Engine
- **Local Persistence**: **Room Database** (Offline-first approach).
- **Cloud Infrastructure**: **Firebase Firestore**.
- **Data Migration Manager**: 
    - **Sync (Push)**: Local changes are automatically pushed to Firestore when online.
    - **Restore (Pull)**: Comprehensive "Download All" feature to restore data from cloud on new device sign-ins.
- **State Management**: **ViewModel** with Repository pattern ensuring single source of truth (Local DB).

### 6.3 Backend / Integration
- **Deep Link Handler**: `link.php` serves as the bridge. It provides a web landing page that triggers the `linkbox://open` intent.
- **Authentication**: Firebase Auth (Anonymous support for immediate friction-less usage).

---

## 7. Database & Schema Reference

### 7.1 Room Entities
- **`EntityItem`**: Represents a file, folder, or page. Contains `parentId` for tree-structure.
- **`SharingEntity`**: Links a local `EntityItem` to a cloud-sharable token. Includes `pointsRequired`.
- **`JoinedLinkEntity`**: Tracks external links that the user has unlocked.
- **`UserPointsEntity`**: Local ledger for currency management.
- **`LinkAnalyticsEntity`**: Tracks views and engagement for shared content.

---

## 8. Development Roadmap & Status
| Phase | Focus | Status |
| :--- | :--- | :--- |
| **Phase 1** | Local DB & CRUD | ✅ Completed |
| **Phase 2** | UI/UX Refinement (Sheets, Animations) | ✅ Completed |
| **Phase 3** | Cloud Sync & Sharing Logic | 🔄 In Progress |
| **Phase 4** | Monetization (Earn Tab + Shop) | 🔄 In Progress |
| **Phase 5** | Social Features (Trending Hub) | 📅 Planned |

---

## 9. Success Metrics
- **Daily Active Users (DAU)**: Engagement on the "Trending" and "Earn" tabs.
- **Conversion Rate**: Percentage of shared links that result in a "Join" action.
- **Retention**: Frequency of users returning to manage their link repository.
