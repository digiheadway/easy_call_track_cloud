# MoviePlanet-23 - Multi-Subdomain Movie Search & Streaming Platform

## 📋 Table of Contents
- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Directory Structure](#directory-structure)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Database Structure](#database-structure)
- [Core Components](#core-components)
- [API Integration](#api-integration)
- [Subdomain System](#subdomain-system)
- [Search Functionality](#search-functionality)
- [Link Generation System](#link-generation-system)
- [Analytics & Tracking](#analytics--tracking)
- [SEO Implementation](#seo-implementation)
- [Security Features](#security-features)
- [Performance Optimization](#performance-optimization)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

MoviePlanet-23 is a sophisticated PHP-based multi-subdomain movie search and streaming platform that dynamically serves content based on subdomain routing. The system aggregates movie/web series search results using Google Custom Search API and provides streaming/download links through TeraBox integration.

### Key Capabilities
- **Multi-subdomain Architecture**: Supports 70+ branded subdomains (filmyzilla, movierulz, ibomma, etc.)
- **Dynamic Image Search**: Automated poster retrieval via Google Custom Search API
- **Smart Caching**: Cookie-based and database caching for optimized performance
- **TeraBox Integration**: Deep link generation for streaming and downloads
- **Analytics Tracking**: Google Analytics 4 (GA4) event tracking throughout user journey
- **SEO Optimized**: Schema.org markup, canonical URLs, and dynamic meta tags
- **Ad Integration**: AdSense and custom ad placement support

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Request                              │
│              (subdomain.domain.com)                          │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│               Apache/Nginx Router                            │
│              (.htaccess rewrites)                            │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│              Subdomain Validator                             │
│          (inc/php/function.php)                              │
│     Check against allowed_subdomains array                   │
└──────────────────┬──────────────────────────────────────────┘
                   │
         ┌─────────┴─────────┐
         │                   │
         ▼                   ▼
┌────────────────┐   ┌──────────────────┐
│  index.php     │   │  msearch.php     │
│  (Home Page)   │   │  (Search Page)   │
└────────┬───────┘   └──────┬───────────┘
         │                   │
         │                   ▼
         │          ┌─────────────────────┐
         │          │  Search Handler      │
         │          │  inc/php/search.php  │
         │          └──────┬──────────────┘
         │                 │
         │        ┌────────┴─────────┐
         │        │                  │
         │        ▼                  ▼
         │   ┌──────────┐      ┌─────────────┐
         │   │Database  │      │ Google CSE  │
         │   │ Cache    │      │    API      │
         │   └──────────┘      └─────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Content Display                           │
│            (Dynamic HTML Generation)                         │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### User-Facing Features
1. **Multi-Brand Interface**: Each subdomain presents a unique branded experience
2. **Smart Search**: Real-time movie/series search with autocomplete
3. **Image Preview**: Automatic poster/thumbnail retrieval
4. **Multiple Streaming Options**:
   - TeraBox Drive App
   - Browser-based viewing
   - Mirror links
5. **Share Functionality**: Social sharing of movie links
6. **Responsive Design**: Mobile and desktop optimized
7. **Dark Theme Support**: Available via CSS

### Backend Features
1. **API Key Rotation**: Automatic cycling through multiple Google CSE API keys
2. **Caching System**:
   - Cookie-based caching (1 hour)
   - Database query caching
   - Featured image caching
3. **Error Handling**:
   - Exhausted API key detection
   - Fallback to cached content
   - 404 error handling
4. **Analytics Integration**:
   - GA4 event tracking
   - Microsoft Clarity integration
   - Custom event logging
5. **Content Management**:
   - Domain-specific content
   - Featured movies section
   - Popular queries tracking

---

## 🛠 Technology Stack

### Core Technologies
- **Backend**: PHP 7.4+ (Object-oriented patterns)
- **Database**: MySQL/MariaDB with mysqli
- **Frontend**: 
  - HTML5 with semantic markup
  - Vanilla CSS (including dark theme)
  - Vanilla JavaScript (ES6+)
- **Web Server**: Apache with mod_rewrite

### Third-Party Integrations
- **Google Custom Search API**: Movie poster retrieval
- **TheMovieDB API**: Movie metadata (via proxy at `/api`)
- **Google Analytics 4**: User behavior tracking
- **Microsoft Clarity**: Session recording
- **Notix**: Web push notifications
- **Google AdSense**: Monetization

### Development Tools
- SVG icons for UI elements
- Google Fonts (Inter)
- Schema.org structured data

---

## 📁 Directory Structure

```
movieplanet-23/
├── inc/                          # Include files
│   ├── php/                      # PHP utilities
│   │   ├── function.php          # Core functions & subdomain handling
│   │   ├── mydb.php              # Database connection config
│   │   ├── search.php            # Search API handler
│   │   ├── correction.php        # Query spelling correction
│   │   ├── find_in_db.php        # Database cache lookup
│   │   ├── find_in_unapproved_queries.php  # Unapproved query handler
│   │   ├── save_queries.php      # Save successful queries
│   │   ├── save_hostname.php     # Track hostnames
│   │   ├── down_tried.php        # Download attempt tracking
│   │   ├── not_this.php          # Wrong result handler
│   │   └── record_hits_for_find_in_db.php  # Hit counter
│   ├── head-global.php           # Global <head> content
│   ├── header.php                # Site header
│   ├── footer.php                # Site footer
│   ├── featured_images.php       # Featured movies carousel
│   ├── other-websites.php        # Cross-promotion links
│   ├── popular_queries.php       # Popular searches
│   ├── tera_link_gen.php         # TeraBox link generator
│   └── img_status.php            # Image status checker
│
├── assets/                       # Static resources
│   ├── css/
│   │   ├── style.css             # Main stylesheet
│   │   ├── popup.css             # Modal/popup styles
│   │   └── dark-theme.css        # Dark mode theme
│   ├── js/
│   │   ├── scriptglobal.js       # Global JS utilities
│   │   └── script.js             # Page-specific scripts
│   └── img/                      # Images, icons, favicons
│
├── content/                      # Domain-specific content
│   ├── default/                  # Fallback content
│   │   └── default.php           # Default page content
│   ├── harleywives/              # Domain-specific folder
│   ├── moviesda10/
│   ├── olamovies/
│   └── cpc-article.php           # CPC ad article
│
├── api/                          # API proxy
│   └── index.php                 # TheMovieDB API proxy
│
├── pages/                        # Static pages
│   ├── contact-us.php
│   ├── taken-down.php            # DMCA/blocked content page
│   └── [other pages]
│
├── epages/                       # External pages
├── ext/                          # External resources
├── tj2/                          # Additional resources
├── trash/                        # Deprecated files
├── teramovies/                   # TeraBox integration files
├── open_in_app/                  # App deep linking
│
├── index.php                     # Homepage
├── msearch.php                   # Main search page
├── msearch2.php                  # Alternative search
├── search.php                    # Legacy search (redirects)
├── search-ss.php                 # Search with screenshots
├── category_search.php           # Category-based search
├── outside-search.php            # External search handler
├── url_shortner.php              # URL shortening service
├── comet-player.php              # Video player page
├── 404.php                       # Custom 404 page
├── .htaccess                     # Apache rewrite rules
├── sitemap-mplanet.xml           # XML sitemap
└── README.md                     # This file
```

---

## 🚀 Installation & Setup

### Prerequisites
```bash
# Server Requirements
- PHP 7.4 or higher
- MySQL 5.7+ or MariaDB 10.3+
- Apache 2.4+ with mod_rewrite enabled
- cURL extension enabled
- SSL certificate (recommended)

# API Keys Required
- Google Custom Search API keys (multiple recommended)
- Google Analytics 4 Property ID
- TheMovieDB API key (for movie metadata)
- Notix Web Push App ID (optional)
```

### Step 1: Clone/Upload Files
```bash
# Upload all files to your web server
# Ensure proper file permissions
chmod 755 /path/to/movieplanet-23
chmod 644 /path/to/movieplanet-23/*.php
```

### Step 2: Database Setup
```sql
-- Create database
CREATE DATABASE fmyfzvvwud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create tables (inferred from code analysis)

-- API Keys Management
CREATE TABLE api_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    requests_made INT DEFAULT 0,
    status ENUM('active', 'exhausted', 'blocked', 'restored_20_MIN') DEFAULT 'active',
    update_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Cached Queries
CREATE TABLE queries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    correct VARCHAR(255) NULL,
    hits INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_query (query)
) ENGINE=InnoDB;

-- Unapproved Queries (backup cache)
CREATE TABLE unapproved_queries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_query (query)
) ENGINE=InnoDB;

-- Extra Info/Logs
CREATE TABLE extra_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Hostname Tracking
CREATE TABLE hostnames (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL UNIQUE,
    hits INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Download Attempts Tracking
CREATE TABLE download_attempts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(255),
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_query (query)
) ENGINE=InnoDB;
```

### Step 3: Configure Database Connection
Edit `inc/php/mydb.php`:
```php
<?php
// Primary Database Server
$servername = "localhost";
$username = "your_db_username";
$password = "your_db_password";
$database = "your_db_name";

// Secondary Database Server (failover)
$servername2 = "localhost";
$username2 = "backup_db_username";
$password2 = "backup_db_password";
$database2 = "backup_db_name";
?>
```

### Step 4: Configure API Keys

#### Add Google Custom Search API Keys
Insert multiple API keys for rotation:
```sql
INSERT INTO api_keys (api_key, status) VALUES 
('YOUR_GOOGLE_API_KEY_1', 'active'),
('YOUR_GOOGLE_API_KEY_2', 'active'),
('YOUR_GOOGLE_API_KEY_3', 'active');
-- Add 10-20 keys for optimal rotation
```

#### Configure TheMovieDB API
Edit `api/index.php`:
```php
$apiKey = 'YOUR_TMDB_API_KEY';
```

#### Configure Google Analytics
Edit `inc/head-global.php`:
```php
gtag("config", "YOUR_GA4_MEASUREMENT_ID");
```

### Step 5: Configure Allowed Subdomains
Edit `inc/php/function.php` to add/remove allowed subdomains:
```php
$allowed_subdomains = array(
    'ibomma',
    'movierulz',
    'filmyzilla',
    // Add your subdomains here
);
```

### Step 6: Setup Subdomain Wildcard
Configure your DNS:
```
Type: A Record
Host: *
Points to: Your_Server_IP
TTL: Automatic/3600
```

Configure Apache virtual host:
```apache
<VirtualHost *:80>
    ServerName domain.com
    ServerAlias *.domain.com
    DocumentRoot /path/to/movieplanet-23
    
    <Directory /path/to/movieplanet-23>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

### Step 7: SSL Setup (Recommended)
```bash
# Using Let's Encrypt with wildcard certificate
certbot certonly --manual \
  --preferred-challenges=dns \
  --email your@email.com \
  --server https://acme-v02.api.letsencrypt.org/directory \
  --agree-tos \
  -d domain.com \
  -d *.domain.com
```

---

## ⚙️ Configuration

### Core Settings (`inc/php/function.php`)

```php
// Subdomain parsing and validation
$arr = explode('.', $_SERVER["SERVER_NAME"]);
$canonical = (empty($_SERVER['HTTPS']) ? 'http://' : 'https://') 
           . $_SERVER['HTTP_HOST'] 
           . strtok($_SERVER['REQUEST_URI'], '?');

$subdomainexactly = $arr[0];  // e.g., "filmyzilla"
$domain = $arr[1];             // e.g., "moviesda10"
$ext = $arr[2];                // e.g., "com"
$fulldomain = $domain . "." . $ext;

// Current month for dynamic titles
$this_month = date("M Y");

// Search URL configuration
$func_search_url = "/msearch.php?" . $_SERVER['QUERY_STRING'];
$func_search_url_prefix = "/msearch.php?q=";

// Title suffix for SEO
$titlesuffix = "Download or Watch Online";
```

### Database Failover Configuration (`inc/php/mydb.php`)
```php
// Automatic failover logic
$conn = @new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
    // Switch to secondary server
    $servername = $servername2;
    $username = $username2;
    $password = $password2;
    $database = $database2;
}
```

### Google Custom Search Configuration (`inc/php/search.php`)
```php
// Search Engine ID (from Google Custom Search)
$searchEngineId = '7117b921e333a4c36';

// Query suffix for better results
$query_suffix = "movie+or+web+series+full+hd+poster";

// API URL format
$url = "https://www.googleapis.com/customsearch/v1"
     . "?key=$apiKey"
     . "&cx=$searchEngineId"
     . "&q=$query+$query_suffix"
     . "&searchType=image";
```

---

## 🗄 Database Structure

### API Keys Table (`api_keys`)
Manages Google Custom Search API key rotation:
- **Tracks**: requests_made, status (active/exhausted/blocked)
- **Auto-restoration**: Keys exhausted for >20 minutes are restored
- **Load balancing**: Queries fetch keys ordered by requests_made ASC

### Queries Table (`queries`)
Primary cache for successful search results:
- **Stores**: query, image_url, hits
- **TTL**: Managed via cookie expiration (1 hour default)
- **Hit tracking**: Increments on each cache hit

### Unapproved Queries Table (`unapproved_queries`)
Fallback cache when primary sources fail:
- **Purpose**: Store results that haven't been verified
- **Usage**: Last resort before showing "not found" image

### Tracking Tables
- **hostnames**: Subdomain usage statistics
- **download_attempts**: User download/watch attempts
- **extra_info**: General logging and error tracking

---

## 🔧 Core Components

### 1. Subdomain Router (`inc/php/function.php`)
**Purpose**: Validates and routes subdomain requests

**Flow**:
```php
1. Parse subdomain from $_SERVER["SERVER_NAME"]
2. Check against $allowed_subdomains array
3. If invalid → 301 redirect to filmyzilla.moviesda10.com
4. If valid → Set subdomain variables and continue
5. Support override via ?dm= parameter or dm cookie
```

**Key Variables**:
- `$subdomainexactly`: Raw subdomain (e.g., "ibomma")
- `$subdomaintitle`: Formatted title (e.g., "Ibomma")
- `$canonical`: Canonical URL for SEO

### 2. Search Handler (`inc/php/search.php`)
**Purpose**: Orchestrates the search flow

**Multi-Layer Cache Strategy**:
```
1. Check cookie cache (instant)
   └─ Found? → Return cached image URL
   
2. Check database cache (inc/php/find_in_db.php)
   └─ Found? → Return image, increment hits
   
3. Check unapproved queries cache
   └─ Found? → Return unapproved image
   
4. Query Google Custom Search API
   ├─ Success → Cache in cookie + DB, return image
   ├─ 429 (Rate Limit) → Mark API key as exhausted
   ├─ 400 (Bad Request) → Block API key
   └─ No Results → Check unapproved queries again
   
5. Final Fallback
   └─ Return /assets/img/not-found.jpg
```

**API Key Rotation Logic**:
```php
// Fetch available keys sorted by usage
$sql = "SELECT * FROM api_keys 
        WHERE status NOT IN ('exhausted', 'blocked') 
        ORDER BY requests_made ASC";

// If active keys < 10, restore exhausted keys (>20 min old)
if ($result->num_rows < 10) {
    $reset = "UPDATE api_keys 
              SET status = 'restored_20_MIN' 
              WHERE status = 'exhausted' 
              AND TIMESTAMPDIFF(MINUTE, update_timestamp, NOW()) > 20";
}

// Try each key until success
while ($row = $result->fetch_assoc()) {
    // Attempt API call
    // Update requests_made on success
    // Mark exhausted/blocked on failure
}
```

### 3. TeraBox Link Generator (`inc/tera_link_gen.php`)
**Purpose**: Creates TeraBox deep links for streaming

**Generated Links**:
```php
// Type 1: App deep link (opens TeraBox app)
$teralink = "terabox://path?query=" . urlencode($searchquery);

// Type 2: Browser link (web-based viewing)
$browserTeraLink1 = "https://browser.terabox.com/share?q=" 
                  . urlencode($searchquery);

// Type 3: Watch link (alternative viewing)
$terawatch = "https://watch.terabox.com/player?title=" 
           . urlencode($searchquery);
```

### 4. Featured Images (`inc/featured_images.php`)
**Purpose**: Displays curated movie carousel on homepage

**Implementation**:
- Static array of featured movies with posters
- Responsive grid layout
- Click tracking via GA4 events

### 5. Content Router (`index.php`, lines 74-93)
**Purpose**: Serves domain/subdomain-specific content

**Routing Logic**:
```php
$folderPath = "content/" . $domain . "/";
$contentfile = $folderPath . $subdomainexactly . ".html";
$defaultFolder = "content/default/";
$defaultfile = $defaultFolder . "default.php";

// Attempt to load domain-specific folder
if (!is_dir($folderPath)) {
    $folderPath = $defaultFolder;
    $contentfile = $folderPath . $subdomainexactly . ".html";
}

// Load subdomain-specific file or default
if (file_exists($contentfile)) {
    include $contentfile;
} else {
    include $defaultfile;
}
```

---

## 🔌 API Integration

### Google Custom Search API
**Configuration**:
- **Search Engine ID**: `7117b921e333a4c36`
- **Search Type**: Image search
- **Query Format**: `{movie_name} movie or web series full hd poster`
- **Rate Limit**: 100 queries/day per key
- **Rotation**: Automatic across 10+ keys

**Response Handling**:
```javascript
// Success
{
  "items": [
    {"link": "https://image-url.com/poster.jpg"}
  ]
}

// Rate Limited
HTTP 429 Too Many Requests

// No Results
{
  "searchInformation": {
    "totalResults": "0"
  }
}
```

### TheMovieDB API (Proxied)
**Proxy Path**: `/api/index.php`

**Purpose**: Hides API key from client requests

**Usage Example**:
```javascript
// Client-side request
fetch('/api/movie/550?language=en-US')
  .then(res => res.json())
  .then(data => console.log(data));

// Proxied to:
https://api.themoviedb.org/3/movie/550?api_key=HIDDEN_KEY&language=en-US
```

**Proxy Features**:
- CORS enabled (`Access-Control-Allow-Origin: *`)
- Method forwarding (GET, POST, OPTIONS)
- Header forwarding
- Compression support (gzip, deflate)
- Connection keep-alive

---

## 🌐 Subdomain System

### Allowed Subdomains (70+)
The system supports these branded movie platforms:
```php
'ibomma', 'movie-rulez-com', 'movierulz', 'soap2day', 
'filmyzilla', 'mp4moviez', 'tamilyogi', 'bolly4u', 
'katmoviehd', 'telegram-web', '123movies', 'extramovies', 
'khatrimaza', '9xmovies', 'tamilrockers', 'fmovies', 
'worldfree4u', 'pluto-tv', 'pirate-bay', 'u-watching-free', 
'vudu', 'yify-movies', 'gomovies', 'peacock-tv', 'putlockers', 
'plex', 'kissanime', 'yesmovies', 'flixtor', 'tubi-tv', 
'primewire', 'moviesjoy', 'tamilgun', '1337x', 'hoopla', 
'solarmovie', 'watch-movies-free', 'fmovies-to', 'roku-channel', 
'vumoo', '0gomovie-s', 'f2movies', 'watchseries', 'crackle', 
'pirate-proxy', 'kanopy', 'popcornflix', 'himovies', 'cmovies', 
'bounce-tv', 'yomovies', 'lookmovie', 'imdb-tv', 'movies123', 
'azmovies', 'fz-movies', 'einthusan', 'vegamovies', 'ytsmovies', 
'watchcartoononline', 'bmovies', 'hdhub4u', 'a-to-z-movies', 
'ato-z-movies', 'isaimini', '7starhd', 'moviesda', 'dvdplay', 
'hdmoviearea', 'tamilblasters', 'moviesflix', 'ipagal'
```

### Subdomain Override System
**Purpose**: Allow external sites to simulate subdomain experience

**Methods**:
1. **Query Parameter**: `?dm=filmyzilla`
2. **Cookie**: `dm=filmyzilla` (set for 1 hour)

**Use Case**: External sites can link users to specific branded experiences

**Example**:
```
https://any-subdomain.domain.com?dm=ibomma
→ Shows "Ibomma" branding even though URL is different
```

---

## 🔍 Search Functionality

### Search Flow (`msearch.php`)

**1. Query Validation**:
```javascript
// Block invalid queries
if (query.includes('https') || query.includes('teralink')) {
    gtag('event', 'searched_invalid_query');
    alert("Search Query not supported");
    redirect('/');
}
```

**2. Query Normalization**:
```php
$searchquery = trim(strtolower($_GET['q']));
$query = urldecode($searchquery);

// Apply spelling correction
include 'inc/php/correction.php';
```

**3. Image Retrieval**:
```php
// Multi-layer cache check (see Search Handler section)
include 'inc/php/search.php';
```

**4. Display Page**:
- Movie poster with loading animation
- Watch/Download buttons
- Share functionality
- "Not This?" search refinement
- File information display

### Search Features

**Autocomplete** (Implemented in `inc/header.php`):
```html
<input type="search" id="dark_search_input" autocomplete="off">
<div id="dark_search_results"></div>
```

**Search Analytics**:
```javascript
// Track search from homepage
gtag('event', 'searched_from_home', {
    'event_label': subdomain
});

// Track "Not This?" searches
gtag('event', 'searched_from_not_this');

// Track no query found
gtag('event', 'no_query_found');
```

---

## 🔗 Link Generation System

### TeraBox Link Types (`inc/tera_link_gen.php`)

**1. App Deep Link**:
```php
$teralink = "terabox://app.terabox.com/share/link?"
          . "surl=" . generateHash($searchquery)
          . "&query=" . urlencode($searchquery);
```

**2. Browser View Link**:
```php
$browserTeraLink1 = "https://www.terabox.com/wap/share/filelist?"
                  . "surl=" . generateHash($searchquery);
```

**3. Mirror Link (Watch)**:
```php
$terawatch = "https://teraboxlink.com/view?"
           . "title=" . urlencode($searchquery . " Full HD");
```

### Link Display (`msearch.php`, lines 111-141)

**Modal Popup Structure**:
```html
<div class="pop23_container" id="pop23_myPopup">
    <!-- TeraBox Drive App Option -->
    <div class="tab" id="popup_tab_app">
        <img src="terabox-icon.png">
        <span>TeraBox Drive App</span>
        <a onclick="gtag('event', 'play_on_tera')">Open</a>
    </div>
    
    <!-- Browser View Option -->
    <div class="tab" id="popup_tab_shortener">
        <img src="chrome-icon.png">
        <span>View in Browser</span>
        <a href="<?php echo $browserTeraLink1; ?>">View</a>
    </div>
    
    <!-- Mirror Link -->
    <div class="tab" id="popup_tab_shortener3">
        <span>Mirror Link 3</span>
        <a href="<?php echo $terawatch; ?>">View</a>
    </div>
</div>
```

### Download Tracking
Every link click is tracked:
```javascript
onclick="down_tried('tera_drive')"  // Logs to download_attempts table
onclick="down_tried('link_shortener')"
onclick="down_tried('mirror_link')"
```

---

## 📊 Analytics & Tracking

### Google Analytics 4 Integration (`inc/head-global.php`)

**Configuration**:
```javascript
gtag("config", "G-6NGMZYQ1WQ");  // Primary property
gtag("config", "G-TMCFD5ECR8");  // Secondary property
```

**Custom Event Function**:
```javascript
function record(event_name, event_info) {
    gtag("event", event_name, {
        event_info: event_info
    });
    console.log(event_name);
}
```

### Tracked Events

**User Journey Events**:
1. `unique_visitor` - First-time visit (localStorage check)
2. `searched_from_home` - Homepage search
3. `searched_from_not_this` - Refined search
4. `watch_play_dialog` - Modal opened
5. `play_on_tera` - TeraBox app clicked (value: ₹0.8)
6. `play_via_url` - Browser view clicked (value: ₹0.4)
7. `not_this` - Wrong result reported

**E-commerce Events** (GA4 Enhanced Ecommerce):
```javascript
gtag("event", "view_cart", {
    currency: "INR",
    items: [{
        item_id: "M_" + query,
        item_name: query,
        item_brand: utm_source,
        affiliation: dm_parameter,
        promotion_name: utm_medium,
        price: 1,
        quantity: 1
    }]
});
```

**Error Events**:
- `searched_invalid_query` - Blocked search terms
- `no_query_found` - Empty search

### Microsoft Clarity Integration
**Session Recording**:
```javascript
(function(c,l,a,r,i,t,y){
    c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
    t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
    y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
})(window, document, "clarity", "script", "jyrsvad036");
```

### Notix Push Notifications
**Web Push Setup**:
```javascript
var s = document.createElement("script");
s.src = "https://notix.io/ent/current/enot.min.js";
s.onload = function (sdk) {
    sdk.startInstall({
        "appId": "10052ebe563441000003bfa0d956e92",
        "loadSettings": true
    });
};
document.head.append(s);
```

---

## 🎯 SEO Implementation

### Schema.org Structured Data

**1. Website Search Schema** (`index.php`, lines 102-114):
```json
{
  "@context": "https://schema.org/",
  "@type": "WebSite",
  "name": "Subdomain Title",
  "url": "https://subdomain.domain.com/",
  "potentialAction": {
    "@type": "SearchAction",
    "target": "https://subdomain.domain.com/get-free-home-loan-quotation.php/?q={search_term_string}",
    "query-input": "required name=search_term_string"
  }
}
```

**2. Organization Schema** (`index.php`, lines 117-127):
```json
{
  "@context": "https://schema.org",
  "@type": "NewsMediaOrganization",
  "name": "Subdomain Title",
  "alternateName": "subdomain",
  "url": "https://subdomain.domain.com/",
  "logo": "",
  "sameAs": "https://www.facebook.com/wikipedia/"
}
```

**3. Article Schema** (`index.php`, lines 130-155):
```json
{
  "@context": "https://schema.org",
  "@type": "Article",
  "mainEntityOfPage": {
    "@type": "WebPage",
    "@id": "https://subdomain.domain.com/"
  },
  "headline": "Subdomain Title - Watch Free Movies",
  "description": "The article is about how Subdomain Title looks...",
  "image": "",
  "author": {
    "@type": "Organization",
    "name": "Contributors to Wikimedia projects"
  },
  "publisher": {
    "@type": "Organization",
    "name": "Subdomain Title"
  },
  "datePublished": "2023-05-03"
}
```

### Meta Tags (`inc/head-global.php` & page templates)

**Essential Meta Tags**:
```html
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="Dynamic description with movie/subdomain">
<meta name="robots" content="index, follow">
<meta name="theme-color" content="#161616">
<meta name="language" content="en">
<meta name="distribution" content="global">
<meta name="author" content="Contributors to Wikimedia projects">
<link rel="canonical" href="<?php echo $canonical; ?>">
```

**Privacy Meta Tags**:
```html
<meta name="referrer" content="never">
<meta name="referrer" content="no-referrer">
```

### Dynamic Titles
**Homepage**:
```php
<title><?php echo $subdomaintitle; ?> <?php echo $this_month; ?> | Watch Or Download Latest Movies/Webseries</title>
```

**Search Page**:
```php
<title><?php echo $searchquery; ?> | <?php echo $titlesuffix; ?></title>
```

### Canonical URLs
```php
$canonical = (empty($_SERVER['HTTPS']) ? 'http://' : 'https://') 
           . $_SERVER['HTTP_HOST'] 
           . strtok($_SERVER['REQUEST_URI'], '?');
```

### XML Sitemap
**Location**: `/sitemap-mplanet.xml`
**Purpose**: Index key pages for search engines

---

## 🔒 Security Features

### 1. Subdomain Validation
**Protection**: Prevents unauthorized subdomain access
```php
if (!in_array($subdomainexactly, $allowed_subdomains)) {
    header('HTTP/1.0 404 Not Found');
    header("Location: https://filmyzilla.moviesda10.com/");
    exit;
}
```

### 2. Query Sanitization
**Protection**: SQL injection prevention
```php
// Search query
$searchquery = trim(strtolower($_GET['q']));
$query = urlencode($searchquery);

// Database queries use prepared statements (where applicable)
$stmt = $conn->prepare("INSERT INTO extra_info (value) VALUES (?)");
$stmt->bind_param("s", $jsonData);
$stmt->execute();
```

### 3. Database Connection Error Suppression
**Protection**: Hides database credentials in error messages
```php
$conn = @new mysqli($servername, $username, $password, $database);
```

### 4. CORS Configuration
**API Proxy**: Controlled CORS for API access
```php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: *");
```

### 5. Invalid Search Protection
**Client-side validation**:
```javascript
if (urlparam('q').includes('https') || 
    urlparam('q').includes('teralink') || 
    urlparam('q').includes('surl')) {
    gtag('event', 'searched invalid query');
    alert("Search Query not supported");
    window.location.href = "/";
}
```

### 6. .htaccess Security

**URL Rewriting**:
```apache
RewriteEngine On

# Redirect specific queries (DMCA compliance)
RewriteCond %{QUERY_STRING} ^q=Dunki [NC]
RewriteRule ^search\.php$ /pages/taken-down.php?utm_source=blocked_resource_dunki [R=301,L]

# 404 for non-existent files
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule . /404.php [R=404,L]
```

### 7. Debug Mode
**Development-only error display**:
```php
if (isset($_GET['debug'])) {
    ini_set('display_errors', 1);
    ini_set('display_startup_errors', 1);
    error_reporting(E_ALL);
}
```

---

## ⚡ Performance Optimization

### 1. Multi-Layer Caching

**Cookie Cache** (Fastest):
```php
// 1-hour cookie cache for image URLs
setcookie($query, $imageUrl, time() + 360000);

// Check cookie first
if (isset($_COOKIE[$query])) {
    echo json_encode(array('imageUrl' => $_COOKIE[$query]));
    die();
}
```

**Database Cache** (Medium):
```php
// Check queries table
$sql = "SELECT image_url, hits FROM queries WHERE query = ?";
// Increment hits on match
```

**Unapproved Queries Cache** (Fallback):
```php
// Use when primary cache misses
include('find_in_unapproved_queries.php');
```

### 2. Asset Optimization

**CSS/JS Versioning** (Cache busting):
```php
<link rel="stylesheet" href="/assets/css/style.css?v=<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/css/style.css'); ?>">
<script src="/assets/js/script.js?v<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/js/script.js'); ?>"></script>
```

**Font Loading**:
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### 3. Database Connection Pooling
**Keep-alive enabled**:
```php
curl_setopt($ch, CURLOPT_FORBID_REUSE, false);
curl_setopt($ch, CURLOPT_FRESH_CONNECT, false);
```

### 4. API Request Optimization

**Compression**:
```php
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Connection: keep-alive',
    'Accept-Encoding: gzip, deflate'
]);
```

**Smart API Key Rotation**:
```php
// Fetch least-used key first
ORDER BY requests_made ASC
```

### 5. Image Loading
**Lazy loading with placeholder**:
```html
<img id="loader_img" 
     src="https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif" 
     alt="Movie" 
     class="v3_movies_fither_img">
```

### 6. Session Management
```php
session_start();
// Minimal session data (only known_user flag)
```

---

## 🚀 Deployment

### Production Checklist

**1. Server Configuration**:
```bash
# Enable required Apache modules
sudo a2enmod rewrite
sudo a2enmod headers
sudo a2enmod ssl

# PHP configuration (php.ini)
upload_max_filesize = 20M
post_max_size = 20M
max_execution_time = 60
memory_limit = 256M
display_errors = Off
log_errors = On
```

**2. Security Hardening**:
```apache
# Add to .htaccess
# Disable directory browsing
Options -Indexes

# Protect sensitive files
<FilesMatch "^(mydb\.php|\.env|\.git)">
    Order allow,deny
    Deny from all
</FilesMatch>

# Force HTTPS
RewriteCond %{HTTPS} off
RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
```

**3. Database Optimization**:
```sql
-- Add indexes for performance
CREATE INDEX idx_query ON queries(query);
CREATE INDEX idx_status ON api_keys(status);
CREATE INDEX idx_requests ON api_keys(requests_made);

-- Regular maintenance
OPTIMIZE TABLE queries;
OPTIMIZE TABLE api_keys;

-- Clean old logs
DELETE FROM extra_info WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

**4. Backup Strategy**:
```bash
# Database backup (cron daily)
0 2 * * * mysqldump -u user -ppassword database > /backups/db_$(date +\%Y\%m\%d).sql

# File backup
0 3 * * * tar -czf /backups/files_$(date +\%Y\%m\%d).tar.gz /path/to/movieplanet-23
```

**5. Monitoring Setup**:
```bash
# Log rotation
/var/log/apache2/*.log {
    daily
    missingok
    rotate 14
    compress
    notifempty
    sharedscripts
}

# Monitor API usage
# Check api_keys.requests_made regularly
# Alert when < 5 active keys
```

**6. Performance Tuning**:
```apache
# Enable Gzip compression
<IfModule mod_deflate.c>
    AddOutputFilterByType DEFLATE text/html text/plain text/xml text/css text/javascript application/javascript
</IfModule>

# Browser caching
<IfModule mod_expires.c>
    ExpiresActive On
    ExpiresByType image/jpeg "access plus 1 year"
    ExpiresByType image/png "access plus 1 year"
    ExpiresByType text/css "access plus 1 month"
    ExpiresByType application/javascript "access plus 1 month"
</IfModule>
```

### Subdomain DNS Setup

**Primary Domain**:
```
Type: A
Host: @
Value: YOUR_SERVER_IP
TTL: 3600
```

**Wildcard Subdomain**:
```
Type: A
Host: *
Value: YOUR_SERVER_IP
TTL: 3600
```

**SSL Certificate**:
```bash
# Wildcard certificate with Certbot
certbot certonly --manual \
  --preferred-challenges=dns \
  --email admin@domain.com \
  --server https://acme-v02.api.letsencrypt.org/directory \
  --agree-tos \
  -d domain.com \
  -d *.domain.com

# Auto-renewal
0 0 1 * * certbot renew --quiet
```

### Environment-Specific Configuration

**Development**:
```php
// Enable errors
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Test database
$servername = "localhost";
$database = "movieplanet_dev";
```

**Production**:
```php
// Disable errors
ini_set('display_errors', 0);
error_reporting(0);

// Production database
$servername = "production-db-server";
$database = "movieplanet_prod";
```

---

## 🔧 Troubleshooting

### Common Issues

**1. No Images Loading**
```
Problem: Google CSE API exhausted
Solution:
1. Check API keys status:
   SELECT api_key, requests_made, status FROM api_keys;
   
2. Add more API keys:
   INSERT INTO api_keys (api_key) VALUES ('NEW_KEY');
   
3. Manually restore exhausted keys:
   UPDATE api_keys SET status = 'active', requests_made = 0 WHERE status = 'exhausted';
```

**2. Subdomain 404 Error**
```
Problem: Subdomain not recognized
Solution:
1. Verify DNS propagation:
   dig subdomain.domain.com
   
2. Check Apache virtual host:
   ServerAlias *.domain.com
   
3. Add subdomain to allowed list:
   Edit inc/php/function.php → $allowed_subdomains
```

**3. Database Connection Errors**
```
Problem: Cannot connect to MySQL
Solution:
1. Check credentials in inc/php/mydb.php
2. Verify MySQL service:
   sudo systemctl status mysql
3. Test connection:
   mysql -u username -p database
4. Check firewall:
   sudo ufw allow 3306/tcp
```

**4. Slow Search Response**
```
Problem: Search takes >5 seconds
Solution:
1. Check database indexes:
   SHOW INDEX FROM queries;
   
2. Optimize tables:
   OPTIMIZE TABLE queries, api_keys;
   
3. Clear old cache entries:
   DELETE FROM queries WHERE updated_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
   
4. Increase API keys:
   Add 10+ more keys for better rotation
```

**5. SSL Certificate Issues**
```
Problem: Mixed content warnings
Solution:
1. Force HTTPS in .htaccess:
   RewriteCond %{HTTPS} off
   RewriteRule ^(.*)$ https://%{HTTP_HOST}/$1 [R=301,L]
   
2. Update canonical URL generation:
   $canonical = 'https://' . $_SERVER['HTTP_HOST'] . strtok($_SERVER['REQUEST_URI'], '?');
```

**6. Analytics Not Tracking**
```
Problem: GA4 events not firing
Solution:
1. Check browser console for errors
2. Verify GA4 Measurement ID in inc/head-global.php
3. Test with GA Debugger Chrome extension
4. Check gtag() function definition
```

### Debug Mode

Enable debug mode by adding `?debug` to any URL:
```
https://subdomain.domain.com/?debug
```

This will:
- Display PHP errors
- Show database query errors
- Log API responses

### Logging

**API Response Logging**:
```php
// Logged to extra_info table
$stmt = $conn->prepare("INSERT INTO extra_info (value) VALUES (?)");
$jsonData = json_encode($responseData);
$stmt->bind_param("s", $jsonData);
$stmt->execute();
```

**Custom Event Logging**:
```php
// Track specific events
$conn->query("INSERT INTO extra_info (value) VALUES ('Active Api Less then " . $result->num_rows . "')");
```

### Support Resources

**Database Schema**:
```sql
-- View table structure
DESCRIBE api_keys;
DESCRIBE queries;
DESCRIBE download_attempts;

-- Check table sizes
SELECT 
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'your_database_name';
```

**API Usage Monitoring**:
```sql
-- Check API key usage
SELECT 
    api_key,
    requests_made,
    status,
    update_timestamp
FROM api_keys
ORDER BY requests_made DESC;

-- Identify top searches
SELECT 
    query,
    hits,
    updated_at
FROM queries
ORDER BY hits DESC
LIMIT 20;
```

---

## 📝 License & Credits

### Third-Party Services
- **Google Custom Search API**: Image search functionality
- **TheMovieDB API**: Movie metadata
- **Google Analytics 4**: User tracking
- **Microsoft Clarity**: Session recording
- **Notix**: Push notifications
- **Google AdSense**: Monetization

### Attribution
Original concept inspired by popular movie streaming aggregator platforms.

### Disclaimer
This software is provided for educational purposes. Users are responsible for ensuring compliance with:
- Copyright laws
- DMCA regulations
- Terms of service for integrated APIs
- Local and international streaming regulations

---

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create feature branch: `git checkout -b feature-name`
3. Make changes and test thoroughly
4. Commit: `git commit -m "Add feature"`
5. Push: `git push origin feature-name`
6. Submit pull request

### Coding Standards
- **PHP**: PSR-12 coding standard
- **JavaScript**: ES6+ features
- **CSS**: BEM naming convention
- **Database**: Normalized schema

### Testing
- Test all subdomains
- Verify API key rotation
- Check caching at all layers
- Test on mobile and desktop
- Validate Schema.org markup

---

## 📞 Support

For issues, questions, or contributions:
- Review this README thoroughly
- Check the Troubleshooting section
- Enable debug mode for detailed error info
- Contact via Telegram: https://t.me/pokipros

---

**Version**: 23.0  
**Last Updated**: 2025  
**Maintained by**: MoviePlanet Development Team

---

## 🎬 Quick Start Summary

```bash
# 1. Install prerequisites
sudo apt install apache2 php mysql-server
sudo a2enmod rewrite

# 2. Configure database
mysql -u root -p < database_schema.sql

# 3. Update credentials
nano inc/php/mydb.php

# 4. Add API keys
mysql -u user -p -e "INSERT INTO database.api_keys (api_key) VALUES ('KEY1'),('KEY2'),('KEY3');"

# 5. Configure subdomain
nano inc/php/function.php  # Add to $allowed_subdomains

# 6. Setup DNS wildcard
# *  A  YOUR_SERVER_IP

# 7. Install SSL
sudo certbot certonly --manual -d domain.com -d *.domain.com

# 8. Start using!
https://filmyzilla.yourdomain.com
```

🎉 **You're all set! The platform is ready to serve movie searches across all configured subdomains.**
